package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.MaintenanceRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Maintenance;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final IrrigationEquipmentRepository equipmentRepository;

    @Transactional
    public Maintenance createMaintenance(Integer farmId, Integer equipmentId, MaintenanceRequest request) {
        IrrigationEquipment equipment = equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", equipmentId + " para la finca ID " + farmId));

        Maintenance maintenance = new Maintenance();
        maintenance.setIrrigationEquipment(equipment);
        maintenance.setDate(request.getDate());
        maintenance.setDescription(request.getDescription());
        maintenance.setWorkHours(request.getWorkHours());

        log.info("Registrando mantenimiento para equipo ID {} en fecha {}", equipmentId, request.getDate());
        return maintenanceRepository.save(maintenance);
    }

    @Transactional(readOnly = true)
    public List<Maintenance> getMaintenancesByEquipment(Integer farmId, Integer equipmentId) {
        IrrigationEquipment equipment = equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", equipmentId + " para la finca ID " + farmId));
        // Asumiendo que MaintenanceRepository tiene findByIrrigationEquipmentOrderByDateDesc
        return maintenanceRepository.findByIrrigationEquipmentOrderByDateDesc(equipment);
    }

    @Transactional(readOnly = true)
    public Maintenance getMaintenanceById(Integer maintenanceId) {
        return maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance", "id", maintenanceId));
    }

    @Transactional
    public Maintenance updateMaintenance(Integer maintenanceId, MaintenanceRequest request) {
        Maintenance maintenance = getMaintenanceById(maintenanceId); // Valida existencia

        // El equipo de un mantenimiento existente no se suele cambiar.
        // Si se necesitara cambiar, hay que validar que el nuevo equipo exista y pertenezca a la misma finca.
        // Por simplicidad, aquí no permitimos cambiar el equipmentId de un mantenimiento.

        maintenance.setDate(request.getDate());
        maintenance.setDescription(request.getDescription());
        maintenance.setWorkHours(request.getWorkHours());

        log.info("Actualizando mantenimiento ID {}", maintenanceId);
        return maintenanceRepository.save(maintenance);
    }

    @Transactional
    public void deleteMaintenance(Integer maintenanceId) {
        Maintenance maintenance = getMaintenanceById(maintenanceId);
        log.warn("Eliminando mantenimiento ID {}", maintenanceId);
        maintenanceRepository.delete(maintenance);
    }
}