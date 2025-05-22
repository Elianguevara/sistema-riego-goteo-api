package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumidityAlertRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumidityAlertResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
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
@RequestMapping() // Ruta base flexible
@RequiredArgsConstructor
@Slf4j
public class HumidityAlertController {

    private final HumidityAlertService humidityAlertService;

    /**
     * Crea una nueva alerta de humedad para un sensor específico.
     */
    @PostMapping("/api/farms/{farmId}/sectors/{sectorId}/humiditysensors/{sensorId}/alerts")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('REGISTRAR_ALERTA_HUMEDAD')") // O podría ser un sistema interno
    public ResponseEntity<?> createHumidityAlert(@PathVariable Integer farmId,
                                                 @PathVariable Integer sectorId,
                                                 @PathVariable Integer sensorId,
                                                 @Valid @RequestBody HumidityAlertRequest request) {
        log.info("Solicitud POST para crear alerta de humedad para sensor ID {} en sector ID {} de finca ID {}", sensorId, sectorId, farmId);
        try {
            HumidityAlert newAlert = humidityAlertService.createHumidityAlert(farmId, sectorId, sensorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new HumidityAlertResponse(newAlert));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear alerta, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al crear alerta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todas las alertas de humedad para un sensor específico.
     */
    @GetMapping("/api/farms/{farmId}/sectors/{sectorId}/humiditysensors/{sensorId}/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_ALERTAS_HUMEDAD')")
    public ResponseEntity<?> getAlertsBySensor(@PathVariable Integer farmId,
                                               @PathVariable Integer sectorId,
                                               @PathVariable Integer sensorId) {
        log.info("Solicitud GET para obtener alertas del sensor ID {} en sector ID {} de finca ID {}", sensorId, sectorId, farmId);
        try {
            List<HumidityAlertResponse> responses = humidityAlertService.getAlertsBySensor(farmId, sectorId, sensorId)
                    .stream()
                    .map(HumidityAlertResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene una alerta de humedad específica por su ID global.
     */
    @GetMapping("/api/humidityalerts/{alertId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_ALERTAS_HUMEDAD')")
    public ResponseEntity<?> getAlertById(@PathVariable Integer alertId) {
        log.info("Solicitud GET para obtener alerta de humedad ID: {}", alertId);
        try {
            HumidityAlert alert = humidityAlertService.getAlertById(alertId);
            // Validar acceso si es necesario
            return ResponseEntity.ok(new HumidityAlertResponse(alert));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza una alerta de humedad existente (uso limitado, ej. para el mensaje).
     */
    @PutMapping("/api/humidityalerts/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_ALERTAS_HUMEDAD')")
    public ResponseEntity<?> updateAlert(@PathVariable Integer alertId,
                                         @Valid @RequestBody HumidityAlertRequest request) {
        // Nota: Considera qué campos de una alerta realmente deberían ser actualizables.
        // Usualmente, los datos que la dispararon (nivel, umbral, fecha) son inmutables.
        log.info("Solicitud PUT para actualizar alerta de humedad ID {}", alertId);
        try {
            HumidityAlert updatedAlert = humidityAlertService.updateAlert(alertId, request);
            return ResponseEntity.ok(new HumidityAlertResponse(updatedAlert));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar alerta, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar alerta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina una alerta de humedad.
     */
    @DeleteMapping("/api/humidityalerts/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_ALERTAS_HUMEDAD')")
    public ResponseEntity<?> deleteAlert(@PathVariable Integer alertId) {
        log.info("Solicitud DELETE para eliminar alerta de humedad ID: {}", alertId);
        try {
            humidityAlertService.deleteAlert(alertId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar alerta, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}