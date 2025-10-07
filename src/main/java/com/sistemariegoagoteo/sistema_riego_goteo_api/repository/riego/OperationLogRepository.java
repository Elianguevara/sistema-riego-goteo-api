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

    /**
     * Busca todas las entradas de la bitácora para una finca específica.
     */
    List<OperationLog> findByFarm(Farm farm);

    /**
     * Busca todas las entradas de la bitácora para una finca en un rango de fechas,
     * ordenadas por fecha descendente.
     */
    List<OperationLog> findByFarmAndOperationDatetimeBetweenOrderByOperationDatetimeDesc(Farm farm, Date startDate, Date endDate);

    /**
     * Busca todas las entradas de la bitácora para una finca,
     * ordenadas por fecha descendente.
     */
    List<OperationLog> findByFarmOrderByOperationDatetimeDesc(Farm farm);
}