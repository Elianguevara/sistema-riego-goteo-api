package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad WaterSource.
 */
@Repository
public interface WaterSourceRepository extends JpaRepository<WaterSource, Integer> {
    List<WaterSource> findByFarm(Farm farm);
    List<WaterSource> findByFarmOrderByTypeAsc(Farm farm);
    Optional<WaterSource> findByTypeAndFarm(String type, Farm farm);
}