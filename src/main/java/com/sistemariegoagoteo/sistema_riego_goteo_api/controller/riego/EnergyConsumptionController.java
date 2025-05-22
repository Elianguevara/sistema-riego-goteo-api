package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.EnergyConsumptionRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.EnergyConsumptionResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.EnergyConsumption;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.EnergyConsumptionService;
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
public class EnergyConsumptionController {

    private final EnergyConsumptionService energyConsumptionService;

    /**
     * Registra un nuevo consumo de energía para una finca específica.
     */
    @PostMapping("/api/farms/{farmId}/energyconsumptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_CONSUMO_ENERGIA')")
    public ResponseEntity<?> createEnergyConsumption(@PathVariable Integer farmId,
                                                     @Valid @RequestBody EnergyConsumptionRequest request) {
        log.info("Solicitud POST para registrar consumo de energía para finca ID {}", farmId);
        try {
            EnergyConsumption newConsumption = energyConsumptionService.createEnergyConsumption(farmId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new EnergyConsumptionResponse(newConsumption));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo registrar consumo, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al registrar consumo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todos los registros de consumo de energía para una finca específica.
     */
    @GetMapping("/api/farms/{farmId}/energyconsumptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_CONSUMO_ENERGIA')")
    public ResponseEntity<?> getEnergyConsumptionsByFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener consumos de energía de la finca ID {}", farmId);
        try {
            List<EnergyConsumptionResponse> responses = energyConsumptionService.getEnergyConsumptionsByFarm(farmId)
                    .stream()
                    .map(EnergyConsumptionResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene un registro de consumo de energía específico por su ID global.
     */
    @GetMapping("/api/energyconsumptions/{consumptionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_CONSUMO_ENERGIA')")
    public ResponseEntity<?> getEnergyConsumptionById(@PathVariable Integer consumptionId) {
        log.info("Solicitud GET para obtener consumo de energía ID: {}", consumptionId);
        try {
            EnergyConsumption consumption = energyConsumptionService.getEnergyConsumptionById(consumptionId);
            // Podrías añadir validación de acceso a la finca si es necesario
            return ResponseEntity.ok(new EnergyConsumptionResponse(consumption));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza un registro de consumo de energía existente.
     */
    @PutMapping("/api/energyconsumptions/{consumptionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_CONSUMO_ENERGIA')")
    public ResponseEntity<?> updateEnergyConsumption(@PathVariable Integer consumptionId,
                                                     @Valid @RequestBody EnergyConsumptionRequest request) {
        log.info("Solicitud PUT para actualizar consumo de energía ID {}", consumptionId);
        try {
            EnergyConsumption updatedConsumption = energyConsumptionService.updateEnergyConsumption(consumptionId, request);
            return ResponseEntity.ok(new EnergyConsumptionResponse(updatedConsumption));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar consumo, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar consumo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un registro de consumo de energía.
     */
    @DeleteMapping("/api/energyconsumptions/{consumptionId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_CONSUMO_ENERGIA')")
    public ResponseEntity<?> deleteEnergyConsumption(@PathVariable Integer consumptionId) {
        log.info("Solicitud DELETE para eliminar consumo de energía ID: {}", consumptionId);
        try {
            energyConsumptionService.deleteEnergyConsumption(consumptionId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar consumo, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}