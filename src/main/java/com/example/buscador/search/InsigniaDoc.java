package com.example.buscador.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "insignias")
public class InsigniaDoc {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String nombre;

    @Field(type = FieldType.Keyword)
    private String categoria;

    @Field(type = FieldType.Integer)
    private Integer stock;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
