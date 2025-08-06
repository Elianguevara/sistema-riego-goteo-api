package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumidityAlertRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumidityAlert;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.HumidityAlertRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.HumiditySensorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HumidityAlertService {

    private final HumidityAlertRepository humidityAlertRepository;
    private final HumiditySensorRepository humiditySensorRepository;
    private final AuditService auditService;

    @Transactional
    public HumidityAlert createHumidityAlert(Integer farmId, Integer sectorId, Integer sensorId, HumidityAlertRequest request) {
        // Primero, validar que el sensor exista y pertenezca al sector y finca correctos
        HumiditySensor sensor = humiditySensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("HumiditySensor", "id", sensorId));

        if (!sensor.getSector().getId().equals(sectorId) || !sensor.getSector().getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("HumiditySensor", "id", sensorId + " para el sector ID " + sectorId + " y finca ID " + farmId);
        }

        HumidityAlert alert = new HumidityAlert();
        alert.setHumiditySensor(sensor);
        alert.setHumidityLevel(request.getHumidityLevel().setScale(2, RoundingMode.HALF_UP));
        alert.setAlertDatetime(request.getAlertDatetime());
        alert.setAlertMessage(request.getAlertMessage());
        alert.setHumidityThreshold(request.getHumidityThreshold().setScale(2, RoundingMode.HALF_UP));

        log.info("Creando alerta de humedad para sensor ID {} ({}%)", sensorId, alert.getHumidityLevel());
        return humidityAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<HumidityAlert> getAlertsBySensor(Integer farmId, Integer sectorId, Integer sensorId) {
        HumiditySensor sensor = humiditySensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("HumiditySensor", "id", sensorId));

        if (!sensor.getSector().getId().equals(sectorId) || !sensor.getSector().getFarm().getId().equals(farmId)) {
             throw new ResourceNotFoundException("HumiditySensor", "id", sensorId + " para el sector ID " + sectorId + " y finca ID " + farmId);
        }
        // Asumiendo que HumidityAlertRepository tiene findByHumiditySensorOrderByAlertDatetimeDesc
        return humidityAlertRepository.findByHumiditySensorOrderByAlertDatetimeDesc(sensor);
    }

    @Transactional(readOnly = true)
    public HumidityAlert getAlertById(Integer alertId) {
        return humidityAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("HumidityAlert", "id", alertId));
    }

    // La actualización de alertas podría ser limitada o no permitida, ya que son registros de eventos.
    // Si se permite, usualmente sería para corregir un mensaje o añadir notas.
    @Transactional
    public HumidityAlert updateAlert(Integer alertId, HumidityAlertRequest request) {
        HumidityAlert alert = getAlertById(alertId);

        // No se debería cambiar el sensor de una alerta existente, ni el nivel de humedad o el umbral que la disparó.
        // Solo se podría permitir actualizar el mensaje, por ejemplo.
        alert.setAlertMessage(request.getAlertMessage());
        // Si se quisiera permitir cambiar la fecha/hora o los niveles (con precaución):
        // alert.setAlertDatetime(request.getAlertDatetime());
        // alert.setHumidityLevel(request.getHumidityLevel().setScale(2, RoundingMode.HALF_UP));
        // alert.setHumidityThreshold(request.getHumidityThreshold().setScale(2, RoundingMode.HALF_UP));

        log.info("Actualizando alerta de humedad ID {}", alertId);
        return humidityAlertRepository.save(alert);
    }


    @Transactional
    public void deleteAlert(Integer alertId) {
        HumidityAlert alert = getAlertById(alertId);
        log.warn("Eliminando alerta de humedad ID {}", alertId);
        humidityAlertRepository.delete(alert);
    }
}