package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad WaterSource.
 */
@Repository
public interface WaterSourceRepository extends JpaRepository<WaterSource, Integer> {
    List<WaterSource> findByFarm(Farm farm);
    List<WaterSource> findByTypeAndFarm(String type, Farm farm);
}