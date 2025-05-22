package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.ReservoirTurn;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repositorio para la entidad ReservoirTurn.
 */
@Repository
public interface ReservoirTurnRepository extends JpaRepository<ReservoirTurn, Integer> {
    List<ReservoirTurn> findByWaterSource(WaterSource waterSource);
    List<ReservoirTurn> findByWaterSourceAndStartDatetimeBetween(WaterSource waterSource, Date startDate, Date endDate);
}