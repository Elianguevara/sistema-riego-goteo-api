package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumiditySensorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.HumiditySensorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORTAR
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects; // <-- IMPORTAR

@Service
@RequiredArgsConstructor
@Slf4j
public class HumiditySensorService {

    private final HumiditySensorRepository humiditySensorRepository;
    private final SectorRepository sectorRepository;
    private final AuditService auditService;

    @Transactional
    public HumiditySensor createHumiditySensor(Integer farmId, Integer sectorId, HumiditySensorRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));

        HumiditySensor sensor = new HumiditySensor();
        sensor.setSector(sector);
        sensor.setSensorType(request.getSensorType());
        sensor.setHumidityLevel(request.getHumidityLevel());
        sensor.setMeasurementDatetime(request.getMeasurementDatetime());

        HumiditySensor savedSensor = humiditySensorRepository.save(sensor);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", HumiditySensor.class.getSimpleName(), "sensorType", null, savedSensor.getSensorType());

        log.info("Creando sensor de humedad tipo '{}' para sector ID {}", request.getSensorType(), sectorId);
        return savedSensor;
    }

    @Transactional
    public HumiditySensor updateHumiditySensor(Integer sensorId, HumiditySensorRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        HumiditySensor sensor = getHumiditySensorById(sensorId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(sensor.getSensorType(), request.getSensorType())) {
            auditService.logChange(currentUser, "UPDATE", HumiditySensor.class.getSimpleName(), "sensorType", sensor.getSensorType(), request.getSensorType());
        }
        if (!Objects.equals(sensor.getHumidityLevel(), request.getHumidityLevel())) {
             auditService.logChange(currentUser, "UPDATE", HumiditySensor.class.getSimpleName(), "humidityLevel", Objects.toString(sensor.getHumidityLevel(), null), Objects.toString(request.getHumidityLevel(), null));
        }
        if (!Objects.equals(sensor.getMeasurementDatetime(), request.getMeasurementDatetime())) {
             auditService.logChange(currentUser, "UPDATE", HumiditySensor.class.getSimpleName(), "measurementDatetime", Objects.toString(sensor.getMeasurementDatetime(), null), Objects.toString(request.getMeasurementDatetime(), null));
        }

        sensor.setSensorType(request.getSensorType());
        sensor.setHumidityLevel(request.getHumidityLevel());
        sensor.setMeasurementDatetime(request.getMeasurementDatetime());

        log.info("Actualizando sensor de humedad ID {}", sensorId);
        return humiditySensorRepository.save(sensor);
    }

    @Transactional
    public void deleteHumiditySensor(Integer sensorId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        HumiditySensor sensor = getHumiditySensorById(sensorId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", HumiditySensor.class.getSimpleName(), "id", sensor.getId().toString(), null);

        log.warn("Eliminando sensor de humedad ID {}", sensorId);
        humiditySensorRepository.delete(sensor);
    }
    
    // --- MÉTODOS GET (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<HumiditySensor> getHumiditySensorsBySector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));
        return humiditySensorRepository.findBySectorOrderBySensorTypeAsc(sector);
    }

    @Transactional(readOnly = true)
    public HumiditySensor getHumiditySensorById(Integer sensorId) {
        return humiditySensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("HumiditySensor", "id", sensorId));
    }
}