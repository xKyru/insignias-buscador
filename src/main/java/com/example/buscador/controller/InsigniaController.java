package com.example.buscador.controller;

import com.example.buscador.model.Insignia;
import com.example.buscador.repository.InsigniaRepository;
import com.example.buscador.search.InsigniaDoc;
import com.example.buscador.search.InsigniaSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

@RestController
@RequestMapping("/insignias")
//@CrossOrigin(origins = "http://localhost:5173") // permite llamadas desde el front
public class InsigniaController {

    private final InsigniaRepository repo;
    private final InsigniaSearchService search;

    public InsigniaController(InsigniaRepository repo, InsigniaSearchService search) {
        this.repo = repo;
        this.search = search;
    }

    // --- Ping para diagnóstico rápido ---
    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }

    // ====== DB (H2/JPA) ======

    // Listar todo o buscar por nombre/categoria en la DB
    @GetMapping
    public List<Insignia> buscarDb(@RequestParam(required = false) String nombre,
                                   @RequestParam(required = false) String categoria) {
        boolean hasNombre = nombre != null && !nombre.isBlank();
        boolean hasCategoria = categoria != null && !categoria.isBlank();

        if (hasNombre) return repo.findByNombreContaining(nombre);
        if (hasCategoria) return repo.findByCategoria(categoria);
        return repo.findAll();
    }

    // Obtener por ID (DB)
    @GetMapping("/{id}")
    public ResponseEntity<Insignia> getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear (guarda en DB e indexa en ES)
    @PostMapping
    public Insignia crear(@RequestBody Insignia i) {
        // forzar nuevo registro
        i.setId(null);
        // valores por defecto razonables
        if (i.getStock() == null) i.setStock(0);

        Insignia saved = repo.save(i);
        // indexar en ES (si está configurado)
        search.index(saved);
        return saved;
    }

    // Actualizar (DB) y reindexar en ES
    @PutMapping("/{id}")
    public ResponseEntity<Insignia> actualizar(@PathVariable Long id, @RequestBody Insignia nueva) {
        return repo.findById(id).map(insignia -> {
            insignia.setNombre(nueva.getNombre());
            insignia.setCategoria(nueva.getCategoria());
            insignia.setStock(nueva.getStock() == null ? 0 : nueva.getStock());
            Insignia updated = repo.save(insignia);
            search.index(updated); // reindexa en ES
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Eliminar (DB) y borrar del índice ES
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        search.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Descontar stock (reserva/compra). Devuelve 200 si descuenta, 409 si no hay stock.
    @PostMapping("/{id}/decrement")
    public ResponseEntity<?> decrementarStock(
            @PathVariable Long id,
            @RequestParam(name = "qty", defaultValue = "1") int qty) {

        if (qty <= 0) {
            return ResponseEntity.badRequest().body("qty debe ser > 0");
        }

        int updated = repo.decrementStock(id, qty);
        if (updated != 1) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Sin stock suficiente o insignia inexistente");
        }

        var opt = repo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Descontó pero no pudo leer la insignia");
        }

        var saved = opt.get();
        try {
            search.index(saved); // mantener ES en sync
        } catch (Exception ignore) {
            // si ES no está, no rompemos la compra
        }
        return ResponseEntity.ok(saved);
    }

    // ====== SEARCH en Elasticsearch ======

    // Búsqueda en ES (por nombre y/o categoria). Si no hay filtros, devuelve todo el índice.
    @GetMapping("/search")
    public List<InsigniaDoc> buscarEs(@RequestParam(required = false) String nombre,
                                      @RequestParam(required = false) String categoria) {
        boolean hasNombre = nombre != null && !nombre.isBlank();
        boolean hasCategoria = categoria != null && !categoria.isBlank();
        return search.search(hasNombre ? nombre : null, hasCategoria ? categoria : null);
    }

    // Reindexar TODA la DB hacia ES (POST recomendado)
    @PostMapping("/reindex")
    public ResponseEntity<String> reindexPost() {
        long count = search.reindexAll();
        return ResponseEntity.ok("Reindexadas " + count + " insignias");
    }

    // (Opcional) Reindex también con GET por comodidad
    @GetMapping("/reindex")
    public ResponseEntity<String> reindexGet() {
        long count = search.reindexAll();
        return ResponseEntity.ok("Reindexadas " + count + " insignias");
    }

    // ====== Manejo simple de errores comunes ======
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handlePkDuplicada(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Violación de integridad (posible ID duplicado). Detalle: " + ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenerico(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error del servidor: " + ex.getMessage());
    }
}
