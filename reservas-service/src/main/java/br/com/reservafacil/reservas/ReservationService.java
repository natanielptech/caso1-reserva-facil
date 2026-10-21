package br.com.reservafacil.reservas;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReservationService(RoomRepository rooms, ReservationRepository reservations) {
        this.rooms = rooms;
        this.reservations = reservations;
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

    @Transactional
    public Reservation criar(CreateReservationRequest request, String hospedeId) {
        validarPeriodo(request.entrada(), request.saida());
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
        return reservations.save(new Reservation(request.hotelId(), request.quartoId(), hospedeId,
                request.entrada(), request.saida(), now, now.plusSeconds(15 * 60)));
    }

    @Transactional
    public Reservation obterDoTitular(Long id, String hospedeId) {
        Reservation reservation = buscar(id);
        autorizar(reservation, hospedeId);
        expirarSeNecessario(reservation);
        return reservation;
    }

    @Transactional
    public Reservation confirmar(Long id, String hospedeId) {
        Reservation reservation = buscar(id);
        autorizar(reservation, hospedeId);
        expirarSeNecessario(reservation);
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

    public record CreateReservationRequest(Long hotelId, Long quartoId, LocalDate entrada, LocalDate saida) {
    }

    public record RoomResponse(Long id, Long hotelId, String nome, java.math.BigDecimal valorDiaria) {
        static RoomResponse from(Room room) {
            return new RoomResponse(room.getId(), room.getHotelId(), room.getNome(), room.getValorDiaria());
        }
    }

    public record ReservationResponse(Long id, Long hotelId, Long quartoId, String hospedeId, LocalDate entrada,
                                       LocalDate saida, ReservationStatus status, Instant criadaEm, Instant expiraEm) {
        static ReservationResponse from(Reservation reservation) {
            return new ReservationResponse(reservation.getId(), reservation.getHotelId(), reservation.getQuartoId(),
                    reservation.getHospedeId(), reservation.getEntrada(), reservation.getSaida(),
                    reservation.getStatus(), reservation.getCriadaEm(), reservation.getExpiraEm());
        }
    }
}
