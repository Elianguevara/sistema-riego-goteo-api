package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationEquipmentRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationEquipmentResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
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
@RequestMapping("/api/farms/{farmId}/equipments") // Anidado bajo fincas
@RequiredArgsConstructor
@Slf4j
public class IrrigationEquipmentController {

    private final IrrigationEquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_EQUIPOS')")
    public ResponseEntity<?> createEquipment(@PathVariable Integer farmId,
                                             @Valid @RequestBody IrrigationEquipmentRequest request) {
        log.info("Solicitud POST para crear equipo en finca ID {}: {}", farmId, request.getName());
        try {
            IrrigationEquipment newEquipment = equipmentService.createEquipment(farmId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new IrrigationEquipmentResponse(newEquipment));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear equipo, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al crear equipo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_EQUIPOS')")
    public ResponseEntity<?> getEquipmentByFarmId(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener equipos de la finca ID: {}", farmId);
        try {
            List<IrrigationEquipmentResponse> responses = equipmentService.getEquipmentByFarmId(farmId).stream()
                    .map(IrrigationEquipmentResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo obtener equipos, finca no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{equipmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_EQUIPOS')")
    public ResponseEntity<?> getEquipmentById(@PathVariable Integer farmId, @PathVariable Integer equipmentId) {
        log.info("Solicitud GET para obtener equipo ID {} de la finca ID: {}", equipmentId, farmId);
        try {
            IrrigationEquipment equipment = equipmentService.getEquipmentByIdAndFarmId(farmId, equipmentId);
            return ResponseEntity.ok(new IrrigationEquipmentResponse(equipment));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{equipmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_EQUIPOS')")
    public ResponseEntity<?> updateEquipment(@PathVariable Integer farmId,
                                             @PathVariable Integer equipmentId,
                                             @Valid @RequestBody IrrigationEquipmentRequest request) {
        log.info("Solicitud PUT para actualizar equipo ID {} en finca ID {}: {}", equipmentId, farmId, request.getName());
        try {
            IrrigationEquipment updatedEquipment = equipmentService.updateEquipment(farmId, equipmentId, request);
            return ResponseEntity.ok(new IrrigationEquipmentResponse(updatedEquipment));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar equipo, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar equipo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{equipmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_EQUIPOS')")
    public ResponseEntity<?> deleteEquipment(@PathVariable Integer farmId, @PathVariable Integer equipmentId) {
        log.info("Solicitud DELETE para eliminar equipo ID {} de la finca ID: {}", equipmentId, farmId);
        try {
            equipmentService.deleteEquipment(farmId, equipmentId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar equipo, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) { // Para la validación de equipo en uso
            log.warn("Intento de eliminación no permitido para equipo ID {}: {}", equipmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409 Conflict es apropiado aquí
        }
    }
}