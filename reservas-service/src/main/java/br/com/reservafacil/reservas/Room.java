package br.com.reservafacil.reservas;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class Room {
    @Id
    private Long id;
    private Long hotelId;
    private String nome;
    private BigDecimal valorDiaria;

    protected Room() {
    }

    public Room(Long id, Long hotelId, String nome, BigDecimal valorDiaria) {
        this.id = id;
        this.hotelId = hotelId;
        this.nome = nome;
        this.valorDiaria = valorDiaria;
    }

    public Long getId() { return id; }
    public Long getHotelId() { return hotelId; }
    public String getNome() { return nome; }
    public BigDecimal getValorDiaria() { return valorDiaria; }
}
