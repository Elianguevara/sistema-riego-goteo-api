package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.SectorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
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
public class SectorService {

    private final SectorRepository sectorRepository;
    private final FarmRepository farmRepository;
    private final IrrigationEquipmentRepository irrigationEquipmentRepository; // Para asociar equipo

    @Transactional
    public Sector createSector(Integer farmId, SectorRequest sectorRequest) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        // Validar si ya existe un sector con el mismo nombre en esta finca (opcional pero recomendado)
        sectorRepository.findByNameAndFarm(sectorRequest.getName(), farm).ifPresent(s -> {
            throw new IllegalArgumentException("Ya existe un sector con el nombre '" + sectorRequest.getName() + "' en la finca '" + farm.getName() + "'.");
        });

        Sector sector = new Sector();
        sector.setName(sectorRequest.getName());
        sector.setFarm(farm);

        if (sectorRequest.getEquipmentId() != null) {
            IrrigationEquipment equipment = irrigationEquipmentRepository.findById(sectorRequest.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", sectorRequest.getEquipmentId()));
            // Validar que el equipo pertenezca a la misma finca que el sector (importante)
            if (!equipment.getFarm().getId().equals(farmId)) {
                throw new IllegalArgumentException("El equipo de irrigación seleccionado no pertenece a la finca especificada.");
            }
            sector.setEquipment(equipment);
        }
        log.info("Creando sector '{}' para la finca ID {}", sector.getName(), farmId);
        return sectorRepository.save(sector);
    }

    @Transactional(readOnly = true)
    public List<Sector> getSectorsByFarmId(Integer farmId) {
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }
        return sectorRepository.findByFarm_Id(farmId); // Asumiendo que tienes findByFarm_Id o findByFarm(Farm farm)
    }

    @Transactional(readOnly = true)
    public Sector getSectorByIdAndFarmId(Integer farmId, Integer sectorId) {
        return sectorRepository.findByIdAndFarm_Id(sectorId, farmId) // Asumiendo método en SectorRepository
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca " + farmId));
    }

    @Transactional
    public Sector updateSector(Integer farmId, Integer sectorId, SectorRequest sectorRequest) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca " + farmId));

        // Validar si el nuevo nombre ya existe en otro sector de esta finca (opcional)
        if (!sector.getName().equals(sectorRequest.getName())) {
            sectorRepository.findByNameAndFarm(sectorRequest.getName(), farm).ifPresent(s -> {
                if (!s.getId().equals(sectorId)) { // Asegurarse que no sea el mismo sector
                     throw new IllegalArgumentException("Ya existe otro sector con el nombre '" + sectorRequest.getName() + "' en la finca '" + farm.getName() + "'.");
                }
            });
        }
        sector.setName(sectorRequest.getName());

        if (sectorRequest.getEquipmentId() != null) {
            IrrigationEquipment equipment = irrigationEquipmentRepository.findById(sectorRequest.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", sectorRequest.getEquipmentId()));
            if (!equipment.getFarm().getId().equals(farmId)) {
                throw new IllegalArgumentException("El equipo de irrigación seleccionado no pertenece a la finca especificada.");
            }
            sector.setEquipment(equipment);
        } else {
            sector.setEquipment(null); // Permitir desasignar equipo
        }
        log.info("Actualizando sector ID {} para la finca ID {}", sectorId, farmId);
        return sectorRepository.save(sector);
    }

    @Transactional
    public void deleteSector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca " + farmId));

        // Validaciones adicionales antes de borrar podrían ir aquí (ej. si tiene irrigaciones activas)
        // La cascada de JPA se encargará de las entidades hijas de Sector (Irrigation, Fertilization, etc.)
        // si Sector.java tiene CascadeType.ALL en esas relaciones.
        log.warn("Eliminando sector ID {} de la finca ID {}", sectorId, farmId);
        sectorRepository.delete(sector);
    }
}