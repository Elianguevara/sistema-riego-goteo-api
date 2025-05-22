package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Sector.
 */
@Repository
public interface SectorRepository extends JpaRepository<Sector, Integer> {
    Optional<Sector> findByNameAndFarm(String name, Farm farm);
    List<Sector> findByFarm(Farm farm);
}