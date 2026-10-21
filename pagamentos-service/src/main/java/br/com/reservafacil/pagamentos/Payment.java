package br.com.reservafacil.pagamentos;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long reservaId;
    private BigDecimal valor;
    private String moeda;
    @Enumerated(EnumType.STRING)
    private PaymentMethod metodo;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String idempotencyKey;
    private Instant criadaEm;

    protected Payment() {
    }

    public Payment(Long reservaId, BigDecimal valor, String moeda, PaymentMethod metodo,
                   PaymentStatus status, String idempotencyKey, Instant criadaEm) {
        this.reservaId = reservaId;
        this.valor = valor;
        this.moeda = moeda;
        this.metodo = metodo;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.criadaEm = criadaEm;
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public BigDecimal getValor() { return valor; }
    public String getMoeda() { return moeda; }
    public PaymentMethod getMetodo() { return metodo; }
    public PaymentStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCriadaEm() { return criadaEm; }
}
