package com.example.buscador.model;

import jakarta.persistence.*;

@Entity
@Table(name = "insignia")
public class Insignia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // <-- importante
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String categoria;

    @Column(nullable = false)
    private Integer stock;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}

