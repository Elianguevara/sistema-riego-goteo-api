package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumidityAlertRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumidityAlert;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.HumidityAlertRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.HumiditySensorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import org.springframework.context.ApplicationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORTAR
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Objects; // <-- IMPORTAR

@Service
@RequiredArgsConstructor
@Slf4j
public class HumidityAlertService {

    private final HumidityAlertRepository humidityAlertRepository;
    private final HumiditySensorRepository humiditySensorRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public HumidityAlert createHumidityAlert(Integer farmId, Integer sectorId, Integer sensorId,
            HumidityAlertRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        HumiditySensor sensor = humiditySensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("HumiditySensor", "id", sensorId));

        if (!sensor.getSector().getId().equals(sectorId) || !sensor.getSector().getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("HumiditySensor", "id",
                    sensorId + " para el sector ID " + sectorId + " y finca ID " + farmId);
        }

        HumidityAlert alert = new HumidityAlert();
        alert.setHumiditySensor(sensor);
        alert.setHumidityLevel(request.getHumidityLevel().setScale(2, RoundingMode.HALF_UP));
        alert.setAlertDatetime(request.getAlertDatetime());
        alert.setAlertMessage(request.getAlertMessage());
        alert.setHumidityThreshold(request.getHumidityThreshold().setScale(2, RoundingMode.HALF_UP));

        HumidityAlert savedAlert = humidityAlertRepository.save(alert);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", HumidityAlert.class.getSimpleName(), "id", null,
                savedAlert.getId().toString());

        // --- EVENTO DE NOTIFICACIÓN ---
        eventPublisher.publishEvent(new com.sistemariegoagoteo.sistema_riego_goteo_api.event.HumidityAlertCreatedEvent(
                savedAlert.getId(),
                farmId,
                "Sensor " + sensor.getId(),
                savedAlert.getHumidityLevel().toString()));

        log.info("Creando alerta de humedad para sensor ID {} ({}%)", sensorId, alert.getHumidityLevel());
        return savedAlert;
    }

    @Transactional
    public HumidityAlert updateAlert(Integer alertId, HumidityAlertRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        HumidityAlert alert = getAlertById(alertId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        // Generalmente, solo el mensaje de una alerta debería ser editable.
        if (!Objects.equals(alert.getAlertMessage(), request.getAlertMessage())) {
            auditService.logChange(currentUser, "UPDATE", HumidityAlert.class.getSimpleName(), "alertMessage",
                    alert.getAlertMessage(), request.getAlertMessage());
        }

        alert.setAlertMessage(request.getAlertMessage());

        log.info("Actualizando alerta de humedad ID {}", alertId);
        return humidityAlertRepository.save(alert);
    }

    @Transactional
    public void deleteAlert(Integer alertId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        HumidityAlert alert = getAlertById(alertId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", HumidityAlert.class.getSimpleName(), "id",
                alert.getId().toString(), null);

        log.warn("Eliminando alerta de humedad ID {}", alertId);
        humidityAlertRepository.delete(alert);
    }

    // --- MÉTODOS GET (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<HumidityAlert> getAlertsBySensor(Integer farmId, Integer sectorId, Integer sensorId) {
        HumiditySensor sensor = humiditySensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("HumiditySensor", "id", sensorId));

        if (!sensor.getSector().getId().equals(sectorId) || !sensor.getSector().getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("HumiditySensor", "id",
                    sensorId + " para el sector ID " + sectorId + " y finca ID " + farmId);
        }
        return humidityAlertRepository.findByHumiditySensorOrderByAlertDatetimeDesc(sensor);
    }

    @Transactional(readOnly = true)
    public HumidityAlert getAlertById(Integer alertId) {
        return humidityAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("HumidityAlert", "id", alertId));
    }
}