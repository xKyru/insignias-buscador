package com.example.buscador.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface InsigniaSearchRepository extends ElasticsearchRepository<InsigniaDoc, String> {
    List<InsigniaDoc> findByNombreContaining(String nombre);
    List<InsigniaDoc> findByCategoria(String categoria);
    List<InsigniaDoc> findByNombreContainingOrCategoriaContaining(String nombre, String categoria);
}
