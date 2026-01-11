package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumidityAlertRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumidityAlertResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumidityAlert;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.HumidityAlertService;
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
public class HumidityAlertController {

    private final HumidityAlertService humidityAlertService;

    @PostMapping("/api/farms/{farmId}/sectors/{sectorId}/humiditysensors/{sensorId}/alerts")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('REGISTRAR_ALERTA_HUMEDAD')")
    public ResponseEntity<HumidityAlertResponse> createHumidityAlert(@PathVariable Integer farmId,
                                                                     @PathVariable Integer sectorId,
                                                                     @PathVariable Integer sensorId,
                                                                     @Valid @RequestBody HumidityAlertRequest request) {
        log.info("Solicitud POST para crear alerta de humedad para sensor ID {} en sector ID {} de finca ID {}", sensorId, sectorId, farmId);
        HumidityAlert newAlert = humidityAlertService.createHumidityAlert(farmId, sectorId, sensorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new HumidityAlertResponse(newAlert));
    }

    @GetMapping("/api/farms/{farmId}/sectors/{sectorId}/humiditysensors/{sensorId}/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_ALERTAS_HUMEDAD')")
    public ResponseEntity<List<HumidityAlertResponse>> getAlertsBySensor(@PathVariable Integer farmId,
                                                                         @PathVariable Integer sectorId,
                                                                         @PathVariable Integer sensorId) {
        log.info("Solicitud GET para obtener alertas del sensor ID {} en sector ID {} de finca ID {}", sensorId, sectorId, farmId);
        List<HumidityAlertResponse> responses = humidityAlertService.getAlertsBySensor(farmId, sectorId, sensorId)
                .stream()
                .map(HumidityAlertResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/api/humidityalerts/{alertId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_ALERTAS_HUMEDAD')")
    public ResponseEntity<HumidityAlertResponse> getAlertById(@PathVariable Integer alertId) {
        log.info("Solicitud GET para obtener alerta de humedad ID: {}", alertId);
        HumidityAlert alert = humidityAlertService.getAlertById(alertId);
        return ResponseEntity.ok(new HumidityAlertResponse(alert));
    }

    @PutMapping("/api/humidityalerts/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_ALERTAS_HUMEDAD')")
    public ResponseEntity<HumidityAlertResponse> updateAlert(@PathVariable Integer alertId,
                                                             @Valid @RequestBody HumidityAlertRequest request) {
        log.info("Solicitud PUT para actualizar alerta de humedad ID {}", alertId);
        HumidityAlert updatedAlert = humidityAlertService.updateAlert(alertId, request);
        return ResponseEntity.ok(new HumidityAlertResponse(updatedAlert));
    }

    @DeleteMapping("/api/humidityalerts/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_ALERTAS_HUMEDAD')")
    public ResponseEntity<Void> deleteAlert(@PathVariable Integer alertId) {
        log.info("Solicitud DELETE para eliminar alerta de humedad ID: {}", alertId);
        humidityAlertService.deleteAlert(alertId);
        return ResponseEntity.noContent().build();
    }
}