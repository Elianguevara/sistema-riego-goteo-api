package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumiditySensorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumiditySensorResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.HumiditySensorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping()
@RequiredArgsConstructor
@Slf4j
public class HumiditySensorController {

    private final HumiditySensorService humiditySensorService;

    @PostMapping("/api/farms/{farmId}/sectors/{sectorId}/humiditysensors")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_SENSORES')")
    public ResponseEntity<HumiditySensorResponse> createHumiditySensor(@PathVariable Integer farmId,
                                                                       @PathVariable Integer sectorId,
                                                                       @Valid @RequestBody HumiditySensorRequest request) {
        log.info("Solicitud POST para crear sensor de humedad para sector ID {} en finca ID {}", sectorId, farmId);
        HumiditySensor newSensor = humiditySensorService.createHumiditySensor(farmId, sectorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new HumiditySensorResponse(newSensor));
    }

    @GetMapping("/api/farms/{farmId}/sectors/{sectorId}/humiditysensors")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_SENSORES')")
    public ResponseEntity<List<HumiditySensorResponse>> getHumiditySensorsBySector(@PathVariable Integer farmId, @PathVariable Integer sectorId) {
        log.info("Solicitud GET para obtener sensores de humedad del sector ID {} en finca ID {}", sectorId, farmId);
        List<HumiditySensorResponse> responses = humiditySensorService.getHumiditySensorsBySector(farmId, sectorId)
                .stream()
                .map(HumiditySensorResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/api/humiditysensors/{sensorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_SENSORES')")
    public ResponseEntity<HumiditySensorResponse> getHumiditySensorById(@PathVariable Integer sensorId) {
        log.info("Solicitud GET para obtener sensor de humedad ID: {}", sensorId);
        HumiditySensor sensor = humiditySensorService.getHumiditySensorById(sensorId);
        return ResponseEntity.ok(new HumiditySensorResponse(sensor));
    }

    @PutMapping("/api/humiditysensors/{sensorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('GESTIONAR_SENSORES') or hasAuthority('REGISTRAR_LECTURA_HUMEDAD')")
    public ResponseEntity<HumiditySensorResponse> updateHumiditySensor(@PathVariable Integer sensorId,
                                                                       @Valid @RequestBody HumiditySensorRequest request) {
        log.info("Solicitud PUT para actualizar sensor de humedad ID {}", sensorId);
        HumiditySensor updatedSensor = humiditySensorService.updateHumiditySensor(sensorId, request);
        return ResponseEntity.ok(new HumiditySensorResponse(updatedSensor));
    }

    @DeleteMapping("/api/humiditysensors/{sensorId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_SENSORES')")
    public ResponseEntity<Void> deleteHumiditySensor(@PathVariable Integer sensorId) {
        log.info("Solicitud DELETE para eliminar sensor de humedad ID: {}", sensorId);
        humiditySensorService.deleteHumiditySensor(sensorId);
        return ResponseEntity.noContent().build();
    }
}