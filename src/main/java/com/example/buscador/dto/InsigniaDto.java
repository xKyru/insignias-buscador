package com.example.buscador.dto;

public class InsigniaDto {
    public Long id;
    public String nombre;
    public String categoria;
    public Integer stock;

    public static InsigniaDto from(com.example.buscador.model.Insignia i) {
        var d = new InsigniaDto();
        d.id = i.getId();
        d.nombre = i.getNombre();
        d.categoria = i.getCategoria();
        d.stock = i.getStock();
        return d;
    }
}
