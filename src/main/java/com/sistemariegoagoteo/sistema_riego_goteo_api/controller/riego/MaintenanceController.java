package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.MaintenanceRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.MaintenanceResponse;
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
@RequestMapping()
@RequiredArgsConstructor
@Slf4j
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/api/farms/{farmId}/equipments/{equipmentId}/maintenances")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_MANTENIMIENTO')")
    public ResponseEntity<MaintenanceResponse> createMaintenance(@PathVariable Integer farmId,
                                                                 @PathVariable Integer equipmentId,
                                                                 @Valid @RequestBody MaintenanceRequest request) {
        log.info("Solicitud POST para registrar mantenimiento para equipo ID {} en finca ID {}", equipmentId, farmId);
        Maintenance newMaintenance = maintenanceService.createMaintenance(farmId, equipmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MaintenanceResponse(newMaintenance));
    }

    @GetMapping("/api/farms/{farmId}/equipments/{equipmentId}/maintenances")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_MANTENIMIENTO')")
    public ResponseEntity<List<MaintenanceResponse>> getMaintenancesByEquipment(@PathVariable Integer farmId, @PathVariable Integer equipmentId) {
        log.info("Solicitud GET para obtener mantenimientos del equipo ID {} en finca ID {}", equipmentId, farmId);
        List<MaintenanceResponse> responses = maintenanceService.getMaintenancesByEquipment(farmId, equipmentId)
                .stream()
                .map(MaintenanceResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/api/maintenances/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_MANTENIMIENTO')")
    public ResponseEntity<MaintenanceResponse> getMaintenanceById(@PathVariable Integer maintenanceId) {
        log.info("Solicitud GET para obtener mantenimiento ID: {}", maintenanceId);
        Maintenance maintenance = maintenanceService.getMaintenanceById(maintenanceId);
        return ResponseEntity.ok(new MaintenanceResponse(maintenance));
    }

    @PutMapping("/api/maintenances/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_MANTENIMIENTO')")
    public ResponseEntity<MaintenanceResponse> updateMaintenance(@PathVariable Integer maintenanceId,
                                                                 @Valid @RequestBody MaintenanceRequest request) {
        log.info("Solicitud PUT para actualizar mantenimiento ID {}", maintenanceId);
        Maintenance updatedMaintenance = maintenanceService.updateMaintenance(maintenanceId, request);
        return ResponseEntity.ok(new MaintenanceResponse(updatedMaintenance));
    }

    @DeleteMapping("/api/maintenances/{maintenanceId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_MANTENIMIENTO')")
    public ResponseEntity<Void> deleteMaintenance(@PathVariable Integer maintenanceId) {
        log.info("Solicitud DELETE para eliminar mantenimiento ID: {}", maintenanceId);
        maintenanceService.deleteMaintenance(maintenanceId);
        return ResponseEntity.noContent().build();
    }
}