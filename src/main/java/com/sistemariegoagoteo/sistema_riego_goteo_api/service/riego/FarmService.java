package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final AuditService auditService;

    
    @Transactional
    public Farm createFarm(FarmRequest farmRequest) {
        Farm farm = new Farm();
        farm.setName(farmRequest.getName());
        farm.setLocation(farmRequest.getLocation());
        farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        farm.setFarmSize(farmRequest.getFarmSize());
        
        Farm savedFarm = farmRepository.save(farm);

        // Registrar modificación para sincronización
        // Usamos el nombre simple de la clase como nombre de la tabla (puedes ajustarlo si tienes un mapeo diferente)
        auditService.recordModificationForSync(Farm.class.getSimpleName(), savedFarm.getId());
        
        return savedFarm;
    }

    @Transactional(readOnly = true)
    public List<Farm> getAllFarms() {
        return farmRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Farm getFarmById(Integer farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
    }

    @Transactional
    public Farm updateFarm(Integer farmId, FarmRequest farmRequest) {
        Farm farm = getFarmById(farmId); // Reutiliza el método para encontrar o lanzar excepción
        farm.setName(farmRequest.getName());
        farm.setLocation(farmRequest.getLocation());
        farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        farm.setFarmSize(farmRequest.getFarmSize());
        
        Farm updatedFarm = farmRepository.save(farm);

        // Registrar modificación para sincronización
        auditService.recordModificationForSync(Farm.class.getSimpleName(), updatedFarm.getId());
        
        return updatedFarm;
    }

    @Transactional
    public void deleteFarm(Integer farmId) {
        Farm farm = getFarmById(farmId);
        // Considera la lógica de eliminación en cascada o validaciones
        // Por ejemplo, ¿qué pasa si una finca tiene sectores asociados?
        farmRepository.delete(farm);
    }

    @Transactional(readOnly = true)
    public List<Farm> findFarmsByUsername(String username) {
        return farmRepository.findFarmsByUsername(username);
    }

}