package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumiditySensorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.HumiditySensorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HumiditySensorService {

    private final HumiditySensorRepository humiditySensorRepository;
    private final SectorRepository sectorRepository;

    @Transactional
    public HumiditySensor createHumiditySensor(Integer farmId, Integer sectorId, HumiditySensorRequest request) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));

        // Opcional: Validar si ya existe un sensor con el mismo tipo en este sector
        // humiditySensorRepository.findBySensorTypeAndSector(request.getSensorType(), sector).ifPresent(s -> {
        //    throw new IllegalArgumentException("Ya existe un sensor de tipo '" + request.getSensorType() + "' en el sector '" + sector.getName() + "'.");
        // });

        HumiditySensor sensor = new HumiditySensor();
        sensor.setSector(sector);
        sensor.setSensorType(request.getSensorType());
        sensor.setHumidityLevel(request.getHumidityLevel()); // Puede ser nulo al crear
        sensor.setMeasurementDatetime(request.getMeasurementDatetime()); // Puede ser nulo al crear

        log.info("Creando sensor de humedad tipo '{}' para sector ID {}", request.getSensorType(), sectorId);
        return humiditySensorRepository.save(sensor);
    }

    @Transactional(readOnly = true)
    public List<HumiditySensor> getHumiditySensorsBySector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));
        // Asumiendo que HumiditySensorRepository tiene findBySectorOrderBySensorTypeAsc
        return humiditySensorRepository.findBySectorOrderBySensorTypeAsc(sector);
    }

    @Transactional(readOnly = true)
    public HumiditySensor getHumiditySensorById(Integer sensorId) {
        return humiditySensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("HumiditySensor", "id", sensorId));
    }

    @Transactional
    public HumiditySensor updateHumiditySensor(Integer sensorId, HumiditySensorRequest request) {
        HumiditySensor sensor = getHumiditySensorById(sensorId); // Valida existencia

        // El sector de un sensor no debería cambiar una vez creado.
        // Si se necesitara, se validaría que el nuevo sector exista y pertenezca a la misma finca.

        sensor.setSensorType(request.getSensorType());
        sensor.setHumidityLevel(request.getHumidityLevel());
        sensor.setMeasurementDatetime(request.getMeasurementDatetime());

        log.info("Actualizando sensor de humedad ID {}", sensorId);
        return humiditySensorRepository.save(sensor);
    }

    // Si se decide tener un método específico para registrar lecturas:
    // @Transactional
    // public HumiditySensor logHumidityReading(Integer sensorId, BigDecimal humidityLevel, Date measurementTime) {
    //     HumiditySensor sensor = getHumiditySensorById(sensorId);
    //     sensor.setHumidityLevel(humidityLevel);
    //     sensor.setMeasurementDatetime(measurementTime);
    //     log.info("Registrando nueva lectura de humedad para sensor ID {}: {}% a las {}", sensorId, humidityLevel, measurementTime);
    //     return humiditySensorRepository.save(sensor);
    // }

    @Transactional
    public void deleteHumiditySensor(Integer sensorId) {
        HumiditySensor sensor = getHumiditySensorById(sensorId);
        // La entidad HumiditySensor tiene cascade = CascadeType.ALL para humidityAlerts.
        // JPA eliminará automáticamente las HumidityAlert asociadas.
        log.warn("Eliminando sensor de humedad ID {}", sensorId);
        humiditySensorRepository.delete(sensor);
    }
}