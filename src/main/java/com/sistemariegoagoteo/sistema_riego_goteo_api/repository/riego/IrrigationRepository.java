package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface IrrigationRepository extends JpaRepository<Irrigation, Integer>, JpaSpecificationExecutor<Irrigation> {
    List<Irrigation> findBySector(Sector sector);
    List<Irrigation> findByEquipment(IrrigationEquipment equipment);
    List<Irrigation> findByStartDatetimeBetween(Date startDate, Date endDate);
    List<Irrigation> findBySectorAndStartDatetimeBetween(Sector sector, Date startDate, Date endDate);
    List<Irrigation> findBySectorOrderByStartDatetimeDesc(Sector sector);
    Optional<Irrigation> findByLocalMobileId(String localMobileId);

    List<Irrigation> findBySectorInAndStartDatetimeBetween(List<Sector> sectors, Date startDate, Date endDate);

    /**
     * Busca todos los riegos de una finca específica dentro de un rango de fechas.
     * Spring Data JPA creará la consulta anidando las propiedades (Sector -> Farm -> Id).
     */
    List<Irrigation> findBySector_Farm_IdAndStartDatetimeBetween(Integer farmId, Date startDate, Date endDate);

    @Query("SELECT s.id as sectorId, s.name as sectorName, " +
            "COALESCE(SUM(i.waterAmount), 0) as totalWaterAmount, " +
            "COALESCE(SUM(i.irrigationHours), 0) as totalIrrigationHours " +
            "FROM Sector s LEFT JOIN s.irrigations i " +
            "WHERE s.farm.id = :farmId AND (s.id IN :sectorIds) " +
            "AND (i.startDatetime IS NULL OR (i.startDatetime BETWEEN :startDate AND :endDate)) " +
            "GROUP BY s.id, s.name")
    List<Object[]> getIrrigationSummaryBySectors(@Param("farmId") Integer farmId,
                                                 @Param("sectorIds") Collection<Integer> sectorIds,
                                                 @Param("startDate") Date startDate,
                                                 @Param("endDate") Date endDate);

    @Query("SELECT FUNCTION('DATE', i.startDatetime) as irrigationDate, " +
            "SUM(i.waterAmount) as dailyWaterAmount, " +
            "SUM(i.irrigationHours) as dailyIrrigationHours " +
            "FROM Irrigation i " +
            "WHERE i.sector.id = :sectorId " +
            "AND i.startDatetime BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', i.startDatetime)")
    List<Object[]> getDailyIrrigationTotals(@Param("sectorId") Integer sectorId,
                                            @Param("startDate") Date startDate,
                                            @Param("endDate") Date endDate);
}