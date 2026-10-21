package br.com.reservafacil.reservas;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long hotelId;
    private Long quartoId;
    private java.math.BigDecimal valorDiaria;
    private String hospedeId;
    private LocalDate entrada;
    private LocalDate saida;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
    private Instant criadaEm;
    private Instant expiraEm;
    private String idempotencyKey;

    protected Reservation() {
    }

    public Reservation(Long hotelId, Long quartoId, java.math.BigDecimal valorDiaria, String hospedeId,
                       LocalDate entrada, LocalDate saida, Instant criadaEm, Instant expiraEm, String idempotencyKey) {
        this.hotelId = hotelId;
        this.quartoId = quartoId;
        this.valorDiaria = valorDiaria;
        this.hospedeId = hospedeId;
        this.entrada = entrada;
        this.saida = saida;
        this.status = ReservationStatus.PRE_RESERVADA;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() { return id; }
    public Long getHotelId() { return hotelId; }
    public Long getQuartoId() { return quartoId; }
    public java.math.BigDecimal getValorDiaria() { return valorDiaria; }
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
