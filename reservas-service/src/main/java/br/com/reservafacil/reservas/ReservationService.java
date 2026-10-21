package br.com.reservafacil.reservas;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final TransactionTemplate transactions;

    public ReservationService(RoomRepository rooms, ReservationRepository reservations,
                              PlatformTransactionManager transactionManager) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> disponibilidade(Long hotelId, LocalDate entrada, LocalDate saida) {
        validarPeriodo(entrada, saida);
        Instant now = Instant.now();
        return rooms.findByHotelId(hotelId).stream()
                .filter(room -> reservations.findByQuartoIdAndStatusIn(room.getId(),
                        List.of(ReservationStatus.PRE_RESERVADA, ReservationStatus.CONFIRMADA)).stream()
                        .noneMatch(reservation -> bloqueia(reservation, entrada, saida, now)))
                .map(RoomResponse::from)
                .toList();
    }

    public synchronized Reservation criar(CreateReservationRequest request, String hospedeId, String idempotencyKey) {
        try {
            return transactions.execute(status -> criarNaTransacao(request, hospedeId, idempotencyKey));
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyKey == null) {
                throw exception;
            }
            Reservation existing = reservations.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing == null) {
                throw exception;
            }
            validarRepeticao(existing, request, hospedeId);
            return existing;
        }
    }

    private Reservation criarNaTransacao(CreateReservationRequest request, String hospedeId, String idempotencyKey) {
        validarPeriodo(request.entrada(), request.saida());
        if (idempotencyKey != null) {
            Reservation existing = reservations.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null) {
                validarRepeticao(existing, request, hospedeId);
                return existing;
            }
        }
        Room room = rooms.findByIdForUpdate(request.quartoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarto não encontrado"));
        if (!room.getHotelId().equals(request.hotelId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarto não encontrado neste hotel");
        }
        Instant now = Instant.now();
        boolean ocupado = reservations.findByQuartoIdAndStatusIn(room.getId(),
                        List.of(ReservationStatus.PRE_RESERVADA, ReservationStatus.CONFIRMADA)).stream()
                .anyMatch(reservation -> bloqueia(reservation, request.entrada(), request.saida(), now));
        if (ocupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quarto indisponível para o período");
        }
        return reservations.save(new Reservation(request.hotelId(), request.quartoId(), room.getValorDiaria(),
                hospedeId, request.entrada(), request.saida(), now, now.plusSeconds(15 * 60), idempotencyKey));
    }

    private void validarRepeticao(Reservation existing, CreateReservationRequest request, String hospedeId) {
        expirarSeNecessario(existing);
        boolean sameRequest = existing.getHospedeId().equals(hospedeId)
                && existing.getHotelId().equals(request.hotelId())
                && existing.getQuartoId().equals(request.quartoId())
                && existing.getEntrada().equals(request.entrada())
                && existing.getSaida().equals(request.saida());
        if (!sameRequest) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency-Key já utilizada em outra solicitação");
        }
        if (existing.getStatus() == ReservationStatus.CANCELADA
                || existing.getStatus() == ReservationStatus.EXPIRADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta tentativa foi encerrada; use uma nova Idempotency-Key");
        }
    }

    @Transactional
    public Reservation obterDoTitular(Long id, String hospedeId) {
        Reservation reservation = buscar(id);
        autorizar(reservation, hospedeId);
        expirarSeNecessario(reservation);
        return reservation;
    }

    @Transactional
    public Reservation obterPorChave(String idempotencyKey, String hospedeId) {
        Reservation reservation = reservations.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada"));
        autorizar(reservation, hospedeId);
        expirarSeNecessario(reservation);
        return reservation;
    }

    @Transactional
    public Reservation confirmar(Long id, String hospedeId) {
        Reservation reservation = buscar(id);
        autorizar(reservation, hospedeId);
        expirarSeNecessario(reservation);
        if (reservation.getStatus() == ReservationStatus.CONFIRMADA) {
            return reservation;
        }
        if (reservation.getStatus() != ReservationStatus.PRE_RESERVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pré-reserva não está mais ativa");
        }
        reservation.confirmar();
        return reservation;
    }

    @Transactional
    public void cancelar(Long id, String hospedeId) {
        Reservation reservation = buscar(id);
        autorizar(reservation, hospedeId);
        if (reservation.getStatus() != ReservationStatus.CANCELADA) {
            reservation.cancelar();
        }
    }

    private Reservation buscar(Long id) {
        return reservations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada"));
    }

    private void autorizar(Reservation reservation, String hospedeId) {
        if (!reservation.getHospedeId().equals(hospedeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reserva pertence a outro hóspede");
        }
    }

    private void expirarSeNecessario(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PRE_RESERVADA
                && !reservation.getExpiraEm().isAfter(Instant.now())) {
            reservation.expirar();
        }
    }

    private boolean bloqueia(Reservation reservation, LocalDate entrada, LocalDate saida, Instant now) {
        if (reservation.getStatus() == ReservationStatus.PRE_RESERVADA
                && !reservation.getExpiraEm().isAfter(now)) {
            reservation.expirar();
            return false;
        }
        return reservation.getEntrada().isBefore(saida) && entrada.isBefore(reservation.getSaida());
    }

    private void validarPeriodo(LocalDate entrada, LocalDate saida) {
        if (entrada == null || saida == null || !entrada.isBefore(saida) || entrada.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A entrada deve ser hoje ou no futuro e anterior à saída");
        }
    }

    public record CreateReservationRequest(@NotNull Long hotelId, @NotNull Long quartoId,
                                           @NotNull LocalDate entrada, @NotNull LocalDate saida) {
    }

    public record RoomResponse(Long id, Long hotelId, String nome, java.math.BigDecimal valorDiaria) {
        static RoomResponse from(Room room) {
            return new RoomResponse(room.getId(), room.getHotelId(), room.getNome(), room.getValorDiaria());
        }
    }

    public record ReservationResponse(Long id, Long hotelId, Long quartoId, java.math.BigDecimal valorDiaria,
                                       String hospedeId, LocalDate entrada,
                                       LocalDate saida, ReservationStatus status, Instant criadaEm, Instant expiraEm) {
        static ReservationResponse from(Reservation reservation) {
            return new ReservationResponse(reservation.getId(), reservation.getHotelId(), reservation.getQuartoId(),
                    reservation.getValorDiaria(), reservation.getHospedeId(), reservation.getEntrada(), reservation.getSaida(),
                    reservation.getStatus(), reservation.getCriadaEm(), reservation.getExpiraEm());
        }
    }
}
