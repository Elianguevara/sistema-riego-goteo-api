package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FertilizationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FertilizationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FertilizationService {

    private final FertilizationRepository fertilizationRepository;
    private final SectorRepository sectorRepository;

    @Transactional
    public Fertilization createFertilization(Integer farmId, Integer sectorId, FertilizationRequest request) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));

        Fertilization fertilization = new Fertilization();
        fertilization.setSector(sector);
        fertilization.setDate(request.getDate());
        fertilization.setFertilizerType(request.getFertilizerType());
        fertilization.setLitersApplied(request.getLitersApplied());

        log.info("Registrando fertilización para sector ID {} en fecha {}", sectorId, request.getDate());
        return fertilizationRepository.save(fertilization);
    }

    @Transactional(readOnly = true)
    public List<Fertilization> getFertilizationsBySector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));
        // Asumiendo que FertilizationRepository tiene findBySectorOrderByDateDesc
        return fertilizationRepository.findBySectorOrderByDateDesc(sector);
    }

    @Transactional(readOnly = true)
    public Fertilization getFertilizationById(Integer fertilizationId) {
        return fertilizationRepository.findById(fertilizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fertilization", "id", fertilizationId));
    }

    @Transactional
    public Fertilization updateFertilization(Integer fertilizationId, FertilizationRequest request) {
        Fertilization fertilization = getFertilizationById(fertilizationId); // Valida existencia

        // El sector de una fertilización existente no se suele cambiar.
        // Si se necesitara cambiar, hay que validar que el nuevo sector exista y pertenezca a la misma finca.
        // Por simplicidad, aquí no permitimos cambiar el sector de una fertilización.

        fertilization.setDate(request.getDate());
        fertilization.setFertilizerType(request.getFertilizerType());
        fertilization.setLitersApplied(request.getLitersApplied());

        log.info("Actualizando fertilización ID {}", fertilizationId);
        return fertilizationRepository.save(fertilization);
    }

    @Transactional
    public void deleteFertilization(Integer fertilizationId) {
        Fertilization fertilization = getFertilizationById(fertilizationId);
        log.warn("Eliminando fertilización ID {}", fertilizationId);
        fertilizationRepository.delete(fertilization);
    }
}