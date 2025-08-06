package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationEquipmentRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrrigationEquipmentService {

    private final IrrigationEquipmentRepository equipmentRepository;
    private final FarmRepository farmRepository;
    private final SectorRepository sectorRepository;
    private final AuditService auditService;

    @Transactional
    public IrrigationEquipment createEquipment(Integer farmId, IrrigationEquipmentRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        
        // ... (validación de nombre existente)

        IrrigationEquipment equipment = new IrrigationEquipment();
        equipment.setName(request.getName());
        equipment.setMeasuredFlow(request.getMeasuredFlow());
        equipment.setHasFlowMeter(request.getHasFlowMeter());
        equipment.setEquipmentType(request.getEquipmentType());
        equipment.setEquipmentStatus(request.getEquipmentStatus());
        equipment.setFarm(farm);

        IrrigationEquipment savedEquipment = equipmentRepository.save(equipment);
        
        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", IrrigationEquipment.class.getSimpleName(), "name", null, savedEquipment.getName());
        // ... auditar otros campos si es necesario ...

        log.info("Creando equipo '{}' para la finca ID {}", equipment.getName(), farmId);
        return savedEquipment;
    }

    @Transactional
    public IrrigationEquipment updateEquipment(Integer farmId, Integer equipmentId, IrrigationEquipmentRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        IrrigationEquipment equipment = equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", equipmentId + " para la finca " + farmId));

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(equipment.getName(), request.getName())) {
             auditService.logChange(currentUser, "UPDATE", IrrigationEquipment.class.getSimpleName(), "name", equipment.getName(), request.getName());
        }
        // ... (comparar y auditar otros campos: measuredFlow, hasFlowMeter, etc.)

        equipment.setName(request.getName());
        equipment.setMeasuredFlow(request.getMeasuredFlow());
        equipment.setHasFlowMeter(request.getHasFlowMeter());
        equipment.setEquipmentType(request.getEquipmentType());
        equipment.setEquipmentStatus(request.getEquipmentStatus());

        log.info("Actualizando equipo ID {} para la finca ID {}", equipmentId, farmId);
        return equipmentRepository.save(equipment);
    }

    @Transactional
    public void deleteEquipment(Integer farmId, Integer equipmentId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        IrrigationEquipment equipment = equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", equipmentId + " para la finca " + farmId));

        if (sectorRepository.existsByEquipment_Id(equipmentId)) {
            throw new IllegalStateException("No se puede eliminar el equipo ID " + equipmentId +
                    " porque está asignado a uno o más sectores. Desasígnelo primero.");
        }
        
        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", IrrigationEquipment.class.getSimpleName(), "id", equipment.getId().toString(), null);

        log.warn("Eliminando equipo ID {} de la finca ID {}", equipmentId, farmId);
        equipmentRepository.delete(equipment);
    }
    
    // ... (los métodos GET no necesitan auditoría de cambios)
    @Transactional(readOnly = true)
    public List<IrrigationEquipment> getEquipmentByFarmId(Integer farmId) {
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }
        return equipmentRepository.findByFarm_Id(farmId);
    }

    @Transactional(readOnly = true)
    public IrrigationEquipment getEquipmentByIdAndFarmId(Integer farmId, Integer equipmentId) {
        return equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", equipmentId + " para la finca " + farmId));
    }
}