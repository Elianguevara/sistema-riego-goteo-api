package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FertilizationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FertilizationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

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
public class FertilizationService {

    private final FertilizationRepository fertilizationRepository;
    private final SectorRepository sectorRepository;
    private final AuditService auditService;

    @Transactional
    public Fertilization createFertilization(Integer farmId, Integer sectorId, FertilizationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));

        Fertilization fertilization = new Fertilization();
        fertilization.setSector(sector);
        fertilization.setDate(request.getDate());
        fertilization.setFertilizerType(request.getFertilizerType());
        fertilization.setLitersApplied(request.getLitersApplied());

        Fertilization savedFertilization = fertilizationRepository.save(fertilization);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", Fertilization.class.getSimpleName(), "fertilizerType", null, savedFertilization.getFertilizerType());
        auditService.logChange(currentUser, "CREATE", Fertilization.class.getSimpleName(), "litersApplied", null, savedFertilization.getLitersApplied().toString());
        
        log.info("Registrando fertilización para sector ID {} en fecha {}", sectorId, request.getDate());
        return savedFertilization;
    }

    @Transactional
    public Fertilization updateFertilization(Integer fertilizationId, FertilizationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Fertilization fertilization = getFertilizationById(fertilizationId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(fertilization.getDate(), request.getDate())) {
            auditService.logChange(currentUser, "UPDATE", Fertilization.class.getSimpleName(), "date", Objects.toString(fertilization.getDate(), null), Objects.toString(request.getDate(), null));
        }
        if (!Objects.equals(fertilization.getFertilizerType(), request.getFertilizerType())) {
            auditService.logChange(currentUser, "UPDATE", Fertilization.class.getSimpleName(), "fertilizerType", fertilization.getFertilizerType(), request.getFertilizerType());
        }
        if (fertilization.getLitersApplied().compareTo(request.getLitersApplied()) != 0) {
            auditService.logChange(currentUser, "UPDATE", Fertilization.class.getSimpleName(), "litersApplied", fertilization.getLitersApplied().toString(), request.getLitersApplied().toString());
        }

        fertilization.setDate(request.getDate());
        fertilization.setFertilizerType(request.getFertilizerType());
        fertilization.setLitersApplied(request.getLitersApplied());

        log.info("Actualizando fertilización ID {}", fertilizationId);
        return fertilizationRepository.save(fertilization);
    }

    @Transactional
    public void deleteFertilization(Integer fertilizationId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Fertilization fertilization = getFertilizationById(fertilizationId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", Fertilization.class.getSimpleName(), "id", fertilization.getId().toString(), null);

        log.warn("Eliminando fertilización ID {}", fertilizationId);
        fertilizationRepository.delete(fertilization);
    }

    // --- MÉTODOS GET (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<Fertilization> getFertilizationsBySector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));
        return fertilizationRepository.findBySectorOrderByDateDesc(sector);
    }

    @Transactional(readOnly = true)
    public Fertilization getFertilizationById(Integer fertilizationId) {
        return fertilizationRepository.findById(fertilizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fertilization", "id", fertilizationId));
    }
}