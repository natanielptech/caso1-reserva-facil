package br.com.reservafacil.hoteis;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Hotel {
    @Id
    private Long id;
    private String nome;
    private String cidade;
    private String fotoUrl;
    private String comodidades;

    protected Hotel() {
    }

    public Hotel(Long id, String nome, String cidade, String fotoUrl, String comodidades) {
        this.id = id;
        this.nome = nome;
        this.cidade = cidade;
        this.fotoUrl = fotoUrl;
        this.comodidades = comodidades;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCidade() { return cidade; }
    public String getFotoUrl() { return fotoUrl; }
    public String getComodidades() { return comodidades; }
}
