package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.EnergyConsumptionRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.EnergyConsumptionResponse;
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
    public ResponseEntity<EnergyConsumptionResponse> createEnergyConsumption(@PathVariable Integer farmId,
                                                                             @Valid @RequestBody EnergyConsumptionRequest request) {
        log.info("Solicitud POST para registrar consumo de energía para finca ID {}", farmId);
        // El servicio lanzará ResourceNotFoundException o IllegalArgumentException si algo falla,
        // y el GlobalExceptionHandler se encargará de responder con el código HTTP correcto.
        EnergyConsumption newConsumption = energyConsumptionService.createEnergyConsumption(farmId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new EnergyConsumptionResponse(newConsumption));
    }

    /**
     * Obtiene todos los registros de consumo de energía para una finca específica.
     */
    @GetMapping("/api/farms/{farmId}/energyconsumptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_CONSUMO_ENERGIA')")
    public ResponseEntity<List<EnergyConsumptionResponse>> getEnergyConsumptionsByFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener consumos de energía de la finca ID {}", farmId);
        List<EnergyConsumptionResponse> responses = energyConsumptionService.getEnergyConsumptionsByFarm(farmId)
                .stream()
                .map(EnergyConsumptionResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene un registro de consumo de energía específico por su ID global.
     */
    @GetMapping("/api/energyconsumptions/{consumptionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_CONSUMO_ENERGIA')")
    public ResponseEntity<EnergyConsumptionResponse> getEnergyConsumptionById(@PathVariable Integer consumptionId) {
        log.info("Solicitud GET para obtener consumo de energía ID: {}", consumptionId);
        EnergyConsumption consumption = energyConsumptionService.getEnergyConsumptionById(consumptionId);
        return ResponseEntity.ok(new EnergyConsumptionResponse(consumption));
    }

    /**
     * Actualiza un registro de consumo de energía existente.
     */
    @PutMapping("/api/energyconsumptions/{consumptionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_CONSUMO_ENERGIA')")
    public ResponseEntity<EnergyConsumptionResponse> updateEnergyConsumption(@PathVariable Integer consumptionId,
                                                                             @Valid @RequestBody EnergyConsumptionRequest request) {
        log.info("Solicitud PUT para actualizar consumo de energía ID {}", consumptionId);
        EnergyConsumption updatedConsumption = energyConsumptionService.updateEnergyConsumption(consumptionId, request);
        return ResponseEntity.ok(new EnergyConsumptionResponse(updatedConsumption));
    }

    /**
     * Elimina un registro de consumo de energía.
     */
    @DeleteMapping("/api/energyconsumptions/{consumptionId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_CONSUMO_ENERGIA')")
    public ResponseEntity<Void> deleteEnergyConsumption(@PathVariable Integer consumptionId) {
        log.info("Solicitud DELETE para eliminar consumo de energía ID: {}", consumptionId);
        energyConsumptionService.deleteEnergyConsumption(consumptionId);
        return ResponseEntity.noContent().build();
    }
}