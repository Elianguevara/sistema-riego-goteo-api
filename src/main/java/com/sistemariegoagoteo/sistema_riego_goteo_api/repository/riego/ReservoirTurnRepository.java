// En ReservoirTurnRepository.java
package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.ReservoirTurn;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ReservoirTurnRepository extends JpaRepository<ReservoirTurn, Integer> {
    List<ReservoirTurn> findByWaterSourceOrderByStartDatetimeDesc(WaterSource waterSource);

    // Ejemplo para validación de solapamiento (puedes ajustarlo o usar especificaciones de JPA)
    @Query("SELECT rt FROM ReservoirTurn rt WHERE rt.waterSource.id = :waterSourceId " +
           "AND rt.id <> :excludeTurnId " + // Excluir el turno actual al actualizar
           "AND ((rt.startDatetime < :newEndDatetime AND rt.endDatetime > :newStartDatetime))")
    List<ReservoirTurn> findOverlappingTurns(
            @Param("waterSourceId") Integer waterSourceId,
            @Param("newStartDatetime") Date newStartDatetime,
            @Param("newEndDatetime") Date newEndDatetime,
            @Param("excludeTurnId") Integer excludeTurnId // Pasa null o -1 si es para creación
    );
}