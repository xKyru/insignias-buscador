package com.example.buscador.repository;

import com.example.buscador.model.Insignia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InsigniaRepository extends JpaRepository<Insignia, Long> {

    // ===== Búsquedas usadas por el controller =====
    List<Insignia> findByNombreContaining(String nombre);
    List<Insignia> findByCategoria(String categoria);

    // ===== Descuento de stock atómico (compra) =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Insignia i set i.stock = i.stock - :qty " +
           "where i.id = :id and i.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);
}
