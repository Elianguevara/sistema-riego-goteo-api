package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.OperationLog;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repositorio para la entidad OperationLog.
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Integer> {
    List<OperationLog> findByFarm(Farm farm);
    List<OperationLog> findByFarmAndStartDatetimeBetween(Farm farm, Date startDate, Date endDate);
    List<OperationLog> findByFarmOrderByStartDatetimeDesc(Farm farm);
    List<OperationLog> findByFarmAndStartDatetimeBetweenOrderByStartDatetimeDesc(Farm farm, Date startDate, Date endDate);
}