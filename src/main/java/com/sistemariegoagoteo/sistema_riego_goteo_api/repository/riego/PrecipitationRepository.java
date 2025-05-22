package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repositorio para la entidad Precipitation.
 */
@Repository
public interface PrecipitationRepository extends JpaRepository<Precipitation, Integer> {
    List<Precipitation> findByFarm(Farm farm);
    List<Precipitation> findByPrecipitationDate(Date date);
    List<Precipitation> findByFarmAndPrecipitationDateBetween(Farm farm, Date startDate, Date endDate);
}