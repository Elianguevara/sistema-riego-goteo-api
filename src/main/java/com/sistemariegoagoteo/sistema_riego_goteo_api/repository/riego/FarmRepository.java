package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Farm.
 */
@Repository
public interface FarmRepository extends JpaRepository<Farm, Integer> {
    // Ejemplo de método personalizado (puedes añadir más según necesidades)
    Optional<Farm> findByName(String name);
    @Query("SELECT f FROM Farm f JOIN f.users u WHERE u.username = :username")
    List<Farm> findFarmsByUsername(@Param("username") String username);
}