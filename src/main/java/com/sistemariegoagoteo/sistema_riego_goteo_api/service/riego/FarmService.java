package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;

    @Transactional
    public Farm createFarm(FarmRequest farmRequest) {
        // Podrías añadir validaciones aquí, como verificar si ya existe una finca con el mismo nombre
        Farm farm = new Farm();
        farm.setName(farmRequest.getName());
        farm.setLocation(farmRequest.getLocation());
        farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        farm.setFarmSize(farmRequest.getFarmSize());
        return farmRepository.save(farm);
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
        return farmRepository.save(farm);
    }

    @Transactional
    public void deleteFarm(Integer farmId) {
        Farm farm = getFarmById(farmId);
        // Considera la lógica de eliminación en cascada o validaciones
        // Por ejemplo, ¿qué pasa si una finca tiene sectores asociados?
        farmRepository.delete(farm);
    }
}