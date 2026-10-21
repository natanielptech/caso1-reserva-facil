package br.com.reservafacil.reservas;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.time.LocalDate;

@Entity
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long hotelId;
    private Long quartoId;
    private String hospedeId;
    private LocalDate entrada;
    private LocalDate saida;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
    private Instant criadaEm;
    private Instant expiraEm;

    protected Reservation() {
    }

    public Reservation(Long hotelId, Long quartoId, String hospedeId, LocalDate entrada,
                       LocalDate saida, Instant criadaEm, Instant expiraEm) {
        this.hotelId = hotelId;
        this.quartoId = quartoId;
        this.hospedeId = hospedeId;
        this.entrada = entrada;
        this.saida = saida;
        this.status = ReservationStatus.PRE_RESERVADA;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    public Long getId() { return id; }
    public Long getHotelId() { return hotelId; }
    public Long getQuartoId() { return quartoId; }
    public String getHospedeId() { return hospedeId; }
    public LocalDate getEntrada() { return entrada; }
    public LocalDate getSaida() { return saida; }
    public ReservationStatus getStatus() { return status; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getExpiraEm() { return expiraEm; }

    public void confirmar() { status = ReservationStatus.CONFIRMADA; }
    public void cancelar() { status = ReservationStatus.CANCELADA; }
    public void expirar() { status = ReservationStatus.EXPIRADA; }
}
