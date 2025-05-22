package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationEquipmentRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrrigationEquipmentService {

    private final IrrigationEquipmentRepository equipmentRepository;
    private final FarmRepository farmRepository;
    private final SectorRepository sectorRepository; // Para validar si el equipo está en uso

    @Transactional
    public IrrigationEquipment createEquipment(Integer farmId, IrrigationEquipmentRequest request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        // Validar si ya existe un equipo con el mismo nombre en esta finca (opcional)
        equipmentRepository.findByNameAndFarm(request.getName(), farm).ifPresent(eq -> {
            throw new IllegalArgumentException("Ya existe un equipo con el nombre '" + request.getName() + "' en la finca '" + farm.getName() + "'.");
        });

        IrrigationEquipment equipment = new IrrigationEquipment();
        equipment.setName(request.getName());
        equipment.setMeasuredFlow(request.getMeasuredFlow());
        equipment.setHasFlowMeter(request.getHasFlowMeter());
        equipment.setEquipmentType(request.getEquipmentType());
        equipment.setEquipmentStatus(request.getEquipmentStatus());
        equipment.setFarm(farm);

        log.info("Creando equipo '{}' para la finca ID {}", equipment.getName(), farmId);
        return equipmentRepository.save(equipment);
    }

    @Transactional(readOnly = true)
    public List<IrrigationEquipment> getEquipmentByFarmId(Integer farmId) {
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }
        // Asumiendo que IrrigationEquipmentRepository tiene findByFarm_Id o findByFarm
        return equipmentRepository.findByFarm_Id(farmId);
    }

    @Transactional(readOnly = true)
    public IrrigationEquipment getEquipmentByIdAndFarmId(Integer farmId, Integer equipmentId) {
        // Asumiendo que IrrigationEquipmentRepository tiene findByIdAndFarm_Id
        return equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", equipmentId + " para la finca " + farmId));
    }

    @Transactional
    public IrrigationEquipment updateEquipment(Integer farmId, Integer equipmentId, IrrigationEquipmentRequest request) {
        // Validar que la finca exista
        farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        IrrigationEquipment equipment = equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", equipmentId + " para la finca " + farmId));

        // Validar si el nuevo nombre ya existe en otro equipo de esta finca (opcional)
        if (!equipment.getName().equals(request.getName())) {
             equipmentRepository.findByNameAndFarm(request.getName(), equipment.getFarm()).ifPresent(eq -> {
                if(!eq.getId().equals(equipmentId)){
                    throw new IllegalArgumentException("Ya existe otro equipo con el nombre '" + request.getName() + "' en la finca '" + equipment.getFarm().getName() + "'.");
                }
             });
        }

        equipment.setName(request.getName());
        equipment.setMeasuredFlow(request.getMeasuredFlow());
        equipment.setHasFlowMeter(request.getHasFlowMeter());
        equipment.setEquipmentType(request.getEquipmentType());
        equipment.setEquipmentStatus(request.getEquipmentStatus());
        // La finca no se cambia en una actualización de equipo usualmente.

        log.info("Actualizando equipo ID {} para la finca ID {}", equipmentId, farmId);
        return equipmentRepository.save(equipment);
    }

    @Transactional
    public void deleteEquipment(Integer farmId, Integer equipmentId) {
        // Validar que la finca exista
         farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        IrrigationEquipment equipment = equipmentRepository.findByIdAndFarm_Id(equipmentId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", equipmentId + " para la finca " + farmId));

        // Validación importante: No permitir eliminar si el equipo está asignado a algún sector.
        // Necesitarás un método en SectorRepository como: boolean existsByEquipment_Id(Integer equipmentId);
        if (sectorRepository.existsByEquipment_Id(equipmentId)) {
            throw new IllegalStateException("No se puede eliminar el equipo ID " + equipmentId +
                    " porque está asignado a uno o más sectores. Desasígnelo primero.");
        }

        // JPA se encargará de la cascada para entidades hijas de IrrigationEquipment (como Maintenance, Irrigation)
        // si IrrigationEquipment.java tiene CascadeType.ALL en esas relaciones.
        log.warn("Eliminando equipo ID {} de la finca ID {}", equipmentId, farmId);
        equipmentRepository.delete(equipment);
    }
}