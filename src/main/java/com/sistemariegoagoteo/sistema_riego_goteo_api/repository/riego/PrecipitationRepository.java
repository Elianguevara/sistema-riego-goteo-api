package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrecipitationRepository extends JpaRepository<Precipitation, Integer> {
    List<Precipitation> findByFarm(Farm farm);
    List<Precipitation> findByPrecipitationDate(Date date);
    List<Precipitation> findByFarmAndPrecipitationDateBetween(Farm farm, Date startDate, Date endDate);
    List<Precipitation> findByFarmOrderByPrecipitationDateDesc(Farm farm);
    Optional<Precipitation> findByFarmAndPrecipitationDate(Farm farm, Date date);

    @Query("SELECT COALESCE(SUM(p.mmRain), 0) FROM Precipitation p WHERE p.farm = :farm AND p.precipitationDate = :date")
    BigDecimal getDailyPrecipitation(@Param("farm") Farm farm, @Param("date") Date date);

    @Query("SELECT COALESCE(SUM(p.mmRain), 0) FROM Precipitation p WHERE p.farm = :farm AND YEAR(p.precipitationDate) = :year AND MONTH(p.precipitationDate) = :month")
    BigDecimal getMonthlyPrecipitation(@Param("farm") Farm farm, @Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(p.mmRain), 0) FROM Precipitation p WHERE p.farm = :farm AND p.precipitationDate BETWEEN :startDate AND :endDate")
    BigDecimal getAnnualPrecipitation(@Param("farm") Farm farm, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // --- MÉTODO NUEVO AÑADIDO ---
    /**
     * Busca todas las precipitaciones de una finca específica dentro de un rango de fechas.
     */
    List<Precipitation> findByFarm_IdAndPrecipitationDateBetween(Integer farmId, Date startDate, Date endDate);
}