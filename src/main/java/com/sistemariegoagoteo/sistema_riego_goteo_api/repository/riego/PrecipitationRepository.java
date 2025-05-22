package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Precipitation.
 */
@Repository
public interface PrecipitationRepository extends JpaRepository<Precipitation, Integer> {
    List<Precipitation> findByFarm(Farm farm);
    List<Precipitation> findByPrecipitationDate(Date date);
    List<Precipitation> findByFarmAndPrecipitationDateBetween(Farm farm, Date startDate, Date endDate);
    List<Precipitation> findByFarmOrderByPrecipitationDateDesc(Farm farm);
    Optional<Precipitation> findByFarmAndPrecipitationDate(Farm farm, Date date); // Útil para evitar duplicados por día/finca si es necesario
}