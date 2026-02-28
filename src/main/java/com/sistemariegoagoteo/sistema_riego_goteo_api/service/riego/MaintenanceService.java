package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.MaintenanceRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Maintenance;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.MaintenanceRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import org.springframework.context.ApplicationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORTAR
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects; // <-- IMPORTAR

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final IrrigationEquipmentRepository equipmentRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Maintenance createMaintenance(Integer farmId, Integer equipmentId, MaintenanceRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        IrrigationEquipment equipment = equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id",
                        equipmentId + " para la finca ID " + farmId));

        Maintenance maintenance = new Maintenance();
        maintenance.setIrrigationEquipment(equipment);
        maintenance.setDate(request.getDate());
        maintenance.setDescription(request.getDescription());
        maintenance.setWorkHours(request.getWorkHours());

        Maintenance savedMaintenance = maintenanceRepository.save(maintenance);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", Maintenance.class.getSimpleName(), "description", null,
                savedMaintenance.getDescription());

        // --- EVENTO DE NOTIFICACIÓN ---
        eventPublisher.publishEvent(new com.sistemariegoagoteo.sistema_riego_goteo_api.event.MaintenanceCreatedEvent(
                savedMaintenance.getId(),
                farmId,
                equipment.getName(),
                savedMaintenance.getDescription()));

        log.info("Registrando mantenimiento para equipo ID {} en fecha {}", equipmentId, request.getDate());
        return savedMaintenance;
    }

    @Transactional
    public Maintenance updateMaintenance(Integer maintenanceId, MaintenanceRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Maintenance maintenance = getMaintenanceById(maintenanceId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(maintenance.getDate(), request.getDate())) {
            auditService.logChange(currentUser, "UPDATE", Maintenance.class.getSimpleName(), "date",
                    Objects.toString(maintenance.getDate(), null), Objects.toString(request.getDate(), null));
        }
        if (!Objects.equals(maintenance.getDescription(), request.getDescription())) {
            auditService.logChange(currentUser, "UPDATE", Maintenance.class.getSimpleName(), "description",
                    maintenance.getDescription(), request.getDescription());
        }
        if (!Objects.equals(maintenance.getWorkHours(), request.getWorkHours())) {
            auditService.logChange(currentUser, "UPDATE", Maintenance.class.getSimpleName(), "workHours",
                    Objects.toString(maintenance.getWorkHours(), null), Objects.toString(request.getWorkHours(), null));
        }

        maintenance.setDate(request.getDate());
        maintenance.setDescription(request.getDescription());
        maintenance.setWorkHours(request.getWorkHours());

        log.info("Actualizando mantenimiento ID {}", maintenanceId);
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public void deleteMaintenance(Integer maintenanceId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Maintenance maintenance = getMaintenanceById(maintenanceId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", Maintenance.class.getSimpleName(), "id",
                maintenance.getId().toString(), null);

        log.warn("Eliminando mantenimiento ID {}", maintenanceId);
        maintenanceRepository.delete(maintenance);
    }

    // --- MÉTODOS GET (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<Maintenance> getMaintenancesByEquipment(Integer farmId, Integer equipmentId) {
        IrrigationEquipment equipment = equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id",
                        equipmentId + " para la finca ID " + farmId));
        return maintenanceRepository.findByIrrigationEquipmentOrderByDateDesc(equipment);
    }

    @Transactional(readOnly = true)
    public Maintenance getMaintenanceById(Integer maintenanceId) {
        return maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance", "id", maintenanceId));
    }
}