package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.MaintenanceRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.MaintenanceResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Maintenance;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.MaintenanceService;
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
@RequestMapping() // Ruta base flexible, definida a nivel de método
@RequiredArgsConstructor
@Slf4j
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    /**
     * Registra un nuevo mantenimiento para un equipo específico de una finca.
     */
    @PostMapping("/api/farms/{farmId}/equipments/{equipmentId}/maintenances")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_MANTENIMIENTO')")
    public ResponseEntity<?> createMaintenance(@PathVariable Integer farmId,
                                               @PathVariable Integer equipmentId,
                                               @Valid @RequestBody MaintenanceRequest request) {
        log.info("Solicitud POST para registrar mantenimiento para equipo ID {} en finca ID {}", equipmentId, farmId);
        try {
            Maintenance newMaintenance = maintenanceService.createMaintenance(farmId, equipmentId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new MaintenanceResponse(newMaintenance));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo registrar mantenimiento, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) { // Si hay otros errores de validación del servicio
            log.warn("Argumento inválido al registrar mantenimiento: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todos los registros de mantenimiento para un equipo específico de una finca.
     */
    @GetMapping("/api/farms/{farmId}/equipments/{equipmentId}/maintenances")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_MANTENIMIENTO')")
    public ResponseEntity<?> getMaintenancesByEquipment(@PathVariable Integer farmId, @PathVariable Integer equipmentId) {
        log.info("Solicitud GET para obtener mantenimientos del equipo ID {} en finca ID {}", equipmentId, farmId);
        try {
            List<MaintenanceResponse> responses = maintenanceService.getMaintenancesByEquipment(farmId, equipmentId)
                    .stream()
                    .map(MaintenanceResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene un registro de mantenimiento específico por su ID global.
     */
    @GetMapping("/api/maintenances/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_MANTENIMIENTO')")
    public ResponseEntity<?> getMaintenanceById(@PathVariable Integer maintenanceId) {
        log.info("Solicitud GET para obtener mantenimiento ID: {}", maintenanceId);
        try {
            Maintenance maintenance = maintenanceService.getMaintenanceById(maintenanceId);
            // Aquí podrías añadir validación para asegurar que el usuario tiene acceso a la finca/equipo de este mantenimiento
            return ResponseEntity.ok(new MaintenanceResponse(maintenance));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza un registro de mantenimiento existente.
     */
    @PutMapping("/api/maintenances/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_MANTENIMIENTO')")
    public ResponseEntity<?> updateMaintenance(@PathVariable Integer maintenanceId,
                                               @Valid @RequestBody MaintenanceRequest request) {
        log.info("Solicitud PUT para actualizar mantenimiento ID {}", maintenanceId);
        try {
            Maintenance updatedMaintenance = maintenanceService.updateMaintenance(maintenanceId, request);
            return ResponseEntity.ok(new MaintenanceResponse(updatedMaintenance));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar mantenimiento, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar mantenimiento: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un registro de mantenimiento.
     */
    @DeleteMapping("/api/maintenances/{maintenanceId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_MANTENIMIENTO')")
    public ResponseEntity<?> deleteMaintenance(@PathVariable Integer maintenanceId) {
        log.info("Solicitud DELETE para eliminar mantenimiento ID: {}", maintenanceId);
        try {
            maintenanceService.deleteMaintenance(maintenanceId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar mantenimiento, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}