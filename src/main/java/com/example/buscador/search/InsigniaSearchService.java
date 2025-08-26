package com.example.buscador.search;

import com.example.buscador.model.Insignia;
import com.example.buscador.repository.InsigniaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class InsigniaSearchService {

    private final InsigniaSearchRepository esRepo;
    private final InsigniaRepository jpaRepo;

    /**
     * Permite desactivar ES desde properties si hace falta:
     * search.es.enabled=false   (por defecto true)
     */
    private final boolean esEnabled;

    public InsigniaSearchService(
            InsigniaSearchRepository esRepo,
            InsigniaRepository jpaRepo,
            @Value("${search.es.enabled:true}") boolean esEnabled
    ) {
        this.esRepo = esRepo;
        this.jpaRepo = jpaRepo;
        this.esEnabled = esEnabled;
    }

    /* =========================
       Operaciones de indexado
       ========================= */

    public void index(Insignia i) {
        if (!esEnabled || i == null) return;
        try {
            esRepo.save(toDoc(i));
        } catch (Exception ignore) {
            // No romper el flujo si ES no está; el buscador por DB sigue funcionando
        }
    }

    public void deleteById(Long id) {
        if (!esEnabled || id == null) return;
        try {
            esRepo.deleteById(String.valueOf(id));
        } catch (Exception ignore) {
        }
    }

    /**
     * Reindexa TODO: borra el índice de ES y lo vuelve a poblar con lo que hay en H2/JPA.
     * Devuelve cuántos documentos quedaron en ES.
     */
    public long reindexAll() {
        if (!esEnabled) return 0L;

        try {
            List<InsigniaDoc> docs = jpaRepo.findAll()
                    .stream()
                    .map(this::toDoc)
                    .collect(Collectors.toList());

            // Limpieza total primero para no dejar “fantasmas”
            esRepo.deleteAll();

            // Bulk save
            esRepo.saveAll(docs);

            return docs.size();
        } catch (Exception ignore) {
            // si ES falla, no tiramos la app
            return 0L;
        }
    }

    /* =========================
       Búsqueda
       ========================= */

    public List<InsigniaDoc> search(String nombre, String categoria) {
        if (!esEnabled) {
            // Si ES está deshabilitado, devolvemos vacío (el front puede seguir mostrando la lista por DB)
            return List.of();
        }
        try {
            boolean hasNombre = nombre != null && !nombre.isBlank();
            boolean hasCategoria = categoria != null && !categoria.isBlank();

            if (hasNombre && hasCategoria) {
                return esRepo.findByNombreContainingOrCategoriaContaining(nombre, categoria);
            }
            if (hasNombre) {
                return esRepo.findByNombreContaining(nombre);
            }
            if (hasCategoria) {
                return esRepo.findByCategoria(categoria);
            }
            // sin filtros -> todo el índice
            return StreamSupport.stream(esRepo.findAll().spliterator(), false)
                    .collect(Collectors.toList());
        } catch (Exception ignore) {
            return List.of();
        }
    }

    /* =========================
       Utilidades / diagnóstico
       ========================= */

    public long countDb() {
        return jpaRepo.count();
    }

    public long countEs() {
        if (!esEnabled) return 0L;
        try {
            return esRepo.count();
        } catch (Exception ignore) {
            return 0L;
        }
    }

    /* =========================
       Mapeo entidad -> doc
       ========================= */
    private InsigniaDoc toDoc(Insignia i) {
        InsigniaDoc d = new InsigniaDoc();
        d.setId(i.getId() != null ? i.getId().toString() : null);
        d.setNombre(i.getNombre());
        d.setCategoria(i.getCategoria());
        d.setStock(i.getStock());
        return d;
    }
}
