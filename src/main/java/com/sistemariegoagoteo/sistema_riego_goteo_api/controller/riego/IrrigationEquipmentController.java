package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationEquipmentRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationEquipmentResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.IrrigationEquipmentService;
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
@RequestMapping("/api/farms/{farmId}/equipments")
@RequiredArgsConstructor
@Slf4j
public class IrrigationEquipmentController {

    private final IrrigationEquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_EQUIPOS')")
    public ResponseEntity<IrrigationEquipmentResponse> createEquipment(@PathVariable Integer farmId,
                                                                       @Valid @RequestBody IrrigationEquipmentRequest request) {
        log.info("Solicitud POST para crear equipo en finca ID {}: {}", farmId, request.getName());
        IrrigationEquipment newEquipment = equipmentService.createEquipment(farmId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new IrrigationEquipmentResponse(newEquipment));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_EQUIPOS')")
    public ResponseEntity<List<IrrigationEquipmentResponse>> getEquipmentByFarmId(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener equipos de la finca ID: {}", farmId);
        List<IrrigationEquipmentResponse> responses = equipmentService.getEquipmentByFarmId(farmId).stream()
                .map(IrrigationEquipmentResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{equipmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_EQUIPOS')")
    public ResponseEntity<IrrigationEquipmentResponse> getEquipmentById(@PathVariable Integer farmId, @PathVariable Integer equipmentId) {
        log.info("Solicitud GET para obtener equipo ID {} de la finca ID: {}", equipmentId, farmId);
        IrrigationEquipment equipment = equipmentService.getEquipmentByIdAndFarmId(farmId, equipmentId);
        return ResponseEntity.ok(new IrrigationEquipmentResponse(equipment));
    }

    @PutMapping("/{equipmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_EQUIPOS')")
    public ResponseEntity<IrrigationEquipmentResponse> updateEquipment(@PathVariable Integer farmId,
                                                                       @PathVariable Integer equipmentId,
                                                                       @Valid @RequestBody IrrigationEquipmentRequest request) {
        log.info("Solicitud PUT para actualizar equipo ID {} en finca ID {}: {}", equipmentId, farmId, request.getName());
        IrrigationEquipment updatedEquipment = equipmentService.updateEquipment(farmId, equipmentId, request);
        return ResponseEntity.ok(new IrrigationEquipmentResponse(updatedEquipment));
    }

    @DeleteMapping("/{equipmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_EQUIPOS')")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Integer farmId, @PathVariable Integer equipmentId) {
        log.info("Solicitud DELETE para eliminar equipo ID {} de la finca ID: {}", equipmentId, farmId);
        equipmentService.deleteEquipment(farmId, equipmentId);
        return ResponseEntity.noContent().build();
    }
}