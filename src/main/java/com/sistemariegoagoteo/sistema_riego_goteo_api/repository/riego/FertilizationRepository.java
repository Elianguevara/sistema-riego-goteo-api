package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repositorio para la entidad Fertilization.
 */
@Repository
public interface FertilizationRepository extends JpaRepository<Fertilization, Integer> {
    List<Fertilization> findBySector(Sector sector);
    List<Fertilization> findByDate(Date date);
    List<Fertilization> findBySectorAndDateBetween(Sector sector, Date startDate, Date endDate);
}