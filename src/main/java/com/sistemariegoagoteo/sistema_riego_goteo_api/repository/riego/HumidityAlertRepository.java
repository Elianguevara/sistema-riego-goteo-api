package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumidityAlert;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repositorio para la entidad HumidityAlert.
 */
@Repository
public interface HumidityAlertRepository extends JpaRepository<HumidityAlert, Integer> {
    List<HumidityAlert> findByHumiditySensor(HumiditySensor humiditySensor);
    List<HumidityAlert> findByAlertDatetimeBetween(Date startDate, Date endDate);
    List<HumidityAlert> findByHumiditySensorAndAlertDatetimeBetween(HumiditySensor humiditySensor, Date startDate, Date endDate);
}