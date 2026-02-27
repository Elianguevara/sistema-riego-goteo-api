package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.SectorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
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

/**
 * Servicio encargado de la gestión de sectores dentro de las fincas.
 * <p>
 * Permite organizar la finca en unidades menores y asignar equipos de riego
 * específicos a cada sector.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SectorService {

    /**
     * Repositorio para la persistencia de sectores.
     */
    private final SectorRepository sectorRepository;

    /**
     * Repositorio para la persistencia de fincas.
     */
    private final FarmRepository farmRepository;

    /**
     * Repositorio para la persistencia de equipos de riego.
     */
    private final IrrigationEquipmentRepository irrigationEquipmentRepository;

    /**
     * Servicio de auditoría para registrar operaciones sobre sectores.
     */
    private final AuditService auditService;

    /**
     * Crea un nuevo sector dentro de una finca específica.
     * Valida que no exista otro sector con el mismo nombre en la misma finca.
     *
     * @param farmId        Identificador de la finca.
     * @param sectorRequest DTO con los datos del nuevo sector.
     * @return El sector creado y persistido.
     * @throws ResourceNotFoundException Si la finca o el equipo no existen.
     */
    @Transactional
    public Sector createSector(Integer farmId, SectorRequest sectorRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        sectorRepository.findByNameAndFarm(sectorRequest.getName(), farm).ifPresent(s -> {
            throw new IllegalArgumentException("Ya existe un sector con el nombre '" + sectorRequest.getName()
                    + "' en la finca '" + farm.getName() + "'.");
        });

        Sector sector = new Sector();
        sector.setName(sectorRequest.getName());
        sector.setFarm(farm);

        if (sectorRequest.getEquipmentId() != null) {
            IrrigationEquipment equipment = irrigationEquipmentRepository.findById(sectorRequest.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id",
                            sectorRequest.getEquipmentId()));
            if (!equipment.getFarm().getId().equals(farmId)) {
                throw new IllegalArgumentException(
                        "El equipo de irrigación seleccionado no pertenece a la finca especificada.");
            }
            sector.setEquipment(equipment);
        }

        Sector savedSector = sectorRepository.save(sector);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", Sector.class.getSimpleName(), "name", null,
                savedSector.getName());
        auditService.logChange(currentUser, "CREATE", Sector.class.getSimpleName(), "farm_id", null, farmId.toString());
        if (savedSector.getEquipment() != null) {
            auditService.logChange(currentUser, "CREATE", Sector.class.getSimpleName(), "equipment_id", null,
                    savedSector.getEquipment().getId().toString());
        }

        log.info("Creando sector '{}' para la finca ID {}", sector.getName(), farmId);
        return savedSector;
    }

    @Transactional
    public Sector updateSector(Integer farmId, Integer sectorId, SectorRequest sectorRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }

        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca " + farmId));

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(sector.getName(), sectorRequest.getName())) {
            auditService.logChange(currentUser, "UPDATE", Sector.class.getSimpleName(), "name", sector.getName(),
                    sectorRequest.getName());
        }

        Integer oldEquipmentId = (sector.getEquipment() != null) ? sector.getEquipment().getId() : null;
        if (!Objects.equals(oldEquipmentId, sectorRequest.getEquipmentId())) {
            auditService.logChange(currentUser, "UPDATE", Sector.class.getSimpleName(), "equipment_id",
                    Objects.toString(oldEquipmentId, null), Objects.toString(sectorRequest.getEquipmentId(), null));
        }

        sector.setName(sectorRequest.getName());

        if (sectorRequest.getEquipmentId() != null) {
            IrrigationEquipment equipment = irrigationEquipmentRepository.findById(sectorRequest.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id",
                            sectorRequest.getEquipmentId()));
            if (!equipment.getFarm().getId().equals(farmId)) {
                throw new IllegalArgumentException(
                        "El equipo de irrigación seleccionado no pertenece a la finca especificada.");
            }
            sector.setEquipment(equipment);
        } else {
            sector.setEquipment(null);
        }

        log.info("Actualizando sector ID {} para la finca ID {}", sectorId, farmId);
        return sectorRepository.save(sector);
    }

    @Transactional
    public void deleteSector(Integer farmId, Integer sectorId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca " + farmId));

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", Sector.class.getSimpleName(), "id", sector.getId().toString(),
                null);

        log.warn("Eliminando sector ID {} de la finca ID {}", sectorId, farmId);
        sectorRepository.delete(sector);
    }

    // --- MÉTODOS GET (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<Sector> getSectorsByFarmId(Integer farmId) {
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }
        return sectorRepository.findByFarm_Id(farmId);
    }

    @Transactional(readOnly = true)
    public Sector getSectorByIdAndFarmId(Integer farmId, Integer sectorId) {
        return sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca " + farmId));
    }

    /**
     * NUEVO MÉTODO: Obtiene todos los sectores considerados "activos".
     * La lógica de negocio define un sector activo como aquel que tiene un equipo
     * de riego asociado en estado "Operativo".
     *
     * @return Una lista de todos los sectores activos en el sistema.
     */
    @Transactional(readOnly = true)
    public List<Sector> getActiveSectors() {
        log.info("Buscando todos los sectores con equipo de riego en estado 'Operativo'");
        // La lógica asume que el estado de un equipo activo es "Operativo"
        return sectorRepository.findSectorsByEquipmentStatus("Activo");
    }
}