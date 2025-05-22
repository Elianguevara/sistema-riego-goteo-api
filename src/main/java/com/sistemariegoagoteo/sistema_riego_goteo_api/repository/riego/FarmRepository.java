package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Farm.
 */
@Repository
public interface FarmRepository extends JpaRepository<Farm, Integer> {
    // Ejemplo de método personalizado (puedes añadir más según necesidades)
    Optional<Farm> findByName(String name);
}