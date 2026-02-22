package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Fertilization> findBySectorOrderByDateDesc(Sector sector);

    // Podrías añadir otros métodos de búsqueda si son necesarios, por ejemplo, por
    // tipo de fertilizante o rango de fechas:
    // List<Fertilization>
    // findBySectorAndFertilizerTypeContainingIgnoreCaseOrderByDateDesc(Sector
    // sector, String fertilizerType);
    // List<Fertilization> findBySectorAndDateBetweenOrderByDateDesc(Sector sector,
    // Date startDate, Date endDate);
    long countBySector_Farm_IdAndDateBetween(Integer farmId, Date startDate, Date endDate);

    @Query("SELECT f.date as datetime, 'FERTILIZACION' as type, " +
            "CONCAT(f.fertilizerType, ': ', f.quantity, ' ', f.quantityUnit) as description, " +
            "CONCAT('Sector: ', f.sector.name) as location, 'N/A' as userName " +
            "FROM Fertilization f WHERE f.sector.farm.id = :farmId AND f.date BETWEEN :startDate AND :endDate")
    List<com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.OperationLogProjection> getFertilizationLogs(
            @Param("farmId") Integer farmId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);
}