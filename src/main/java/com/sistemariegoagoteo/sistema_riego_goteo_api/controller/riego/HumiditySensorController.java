package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumiditySensorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumiditySensorResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
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
@RequestMapping() // Ruta base flexible
@RequiredArgsConstructor
@Slf4j
public class HumiditySensorController {

    private final HumiditySensorService humiditySensorService;

    /**
     * Crea un nuevo sensor de humedad para un sector específico de una finca.
     */
    @PostMapping("/api/farms/{farmId}/sectors/{sectorId}/humiditysensors")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_SENSORES')")
    public ResponseEntity<?> createHumiditySensor(@PathVariable Integer farmId,
                                                  @PathVariable Integer sectorId,
                                                  @Valid @RequestBody HumiditySensorRequest request) {
        log.info("Solicitud POST para crear sensor de humedad para sector ID {} en finca ID {}", sectorId, farmId);
        try {
            HumiditySensor newSensor = humiditySensorService.createHumiditySensor(farmId, sectorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new HumiditySensorResponse(newSensor));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear sensor, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al crear sensor: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todos los sensores de humedad para un sector específico de una finca.
     */
    @GetMapping("/api/farms/{farmId}/sectors/{sectorId}/humiditysensors")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_SENSORES')")
    public ResponseEntity<?> getHumiditySensorsBySector(@PathVariable Integer farmId, @PathVariable Integer sectorId) {
        log.info("Solicitud GET para obtener sensores de humedad del sector ID {} en finca ID {}", sectorId, farmId);
        try {
            List<HumiditySensorResponse> responses = humiditySensorService.getHumiditySensorsBySector(farmId, sectorId)
                    .stream()
                    .map(HumiditySensorResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene un sensor de humedad específico por su ID global.
     */
    @GetMapping("/api/humiditysensors/{sensorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_SENSORES')")
    public ResponseEntity<?> getHumiditySensorById(@PathVariable Integer sensorId) {
        log.info("Solicitud GET para obtener sensor de humedad ID: {}", sensorId);
        try {
            HumiditySensor sensor = humiditySensorService.getHumiditySensorById(sensorId);
            // Validar acceso a la finca/sector si es necesario
            return ResponseEntity.ok(new HumiditySensorResponse(sensor));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza un sensor de humedad existente (incluyendo el registro de una nueva lectura).
     */
    @PutMapping("/api/humiditysensors/{sensorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('GESTIONAR_SENSORES') or hasAuthority('REGISTRAR_LECTURA_HUMEDAD')")
    public ResponseEntity<?> updateHumiditySensor(@PathVariable Integer sensorId,
                                                  @Valid @RequestBody HumiditySensorRequest request) {
        log.info("Solicitud PUT para actualizar sensor de humedad ID {}", sensorId);
        try {
            HumiditySensor updatedSensor = humiditySensorService.updateHumiditySensor(sensorId, request);
            return ResponseEntity.ok(new HumiditySensorResponse(updatedSensor));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar sensor, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar sensor: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un sensor de humedad.
     */
    @DeleteMapping("/api/humiditysensors/{sensorId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_SENSORES')")
    public ResponseEntity<?> deleteHumiditySensor(@PathVariable Integer sensorId) {
        log.info("Solicitud DELETE para eliminar sensor de humedad ID: {}", sensorId);
        try {
            humiditySensorService.deleteHumiditySensor(sensorId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar sensor, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}