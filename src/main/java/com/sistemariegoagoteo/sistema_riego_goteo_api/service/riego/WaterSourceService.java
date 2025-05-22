package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.WaterSourceRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.WaterSourceRepository;
// Importar ReservoirTurnRepository si se necesitan validaciones específicas antes de borrar
// import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.ReservoirTurnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaterSourceService {

    private final WaterSourceRepository waterSourceRepository;
    private final FarmRepository farmRepository;
    // private final ReservoirTurnRepository reservoirTurnRepository; // Descomentar si se usa

    @Transactional
    public WaterSource createWaterSource(Integer farmId, WaterSourceRequest request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        // Opcional: Validar si ya existe una fuente de agua con el mismo tipo en esta finca
        waterSourceRepository.findByTypeAndFarm(request.getType(), farm).ifPresent(ws -> {
            throw new IllegalArgumentException("Ya existe una fuente de agua de tipo '" + request.getType() + "' en la finca '" + farm.getName() + "'.");
        });

        WaterSource waterSource = new WaterSource();
        waterSource.setFarm(farm);
        waterSource.setType(request.getType());

        log.info("Creando fuente de agua tipo '{}' para finca ID {}", request.getType(), farmId);
        return waterSourceRepository.save(waterSource);
    }

    @Transactional(readOnly = true)
    public List<WaterSource> getWaterSourcesByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        // Asumiendo que WaterSourceRepository tiene findByFarmOrderByTypeAsc
        return waterSourceRepository.findByFarmOrderByTypeAsc(farm);
    }

    @Transactional(readOnly = true)
    public WaterSource getWaterSourceById(Integer waterSourceId) {
        return waterSourceRepository.findById(waterSourceId)
                .orElseThrow(() -> new ResourceNotFoundException("WaterSource", "id", waterSourceId));
    }

    @Transactional
    public WaterSource updateWaterSource(Integer waterSourceId, WaterSourceRequest request) {
        WaterSource waterSource = getWaterSourceById(waterSourceId); // Valida existencia

        // La finca de una fuente de agua no debería cambiar.
        // Validar si el nuevo tipo ya existe para la misma finca, excluyendo la actual.
        if (!waterSource.getType().equalsIgnoreCase(request.getType())) {
            waterSourceRepository.findByTypeAndFarm(request.getType(), waterSource.getFarm()).ifPresent(ws -> {
                if (!ws.getId().equals(waterSourceId)) {
                    throw new IllegalArgumentException("Ya existe otra fuente de agua de tipo '" + request.getType() +
                            "' en la finca '" + waterSource.getFarm().getName() + "'.");
                }
            });
        }
        waterSource.setType(request.getType());

        log.info("Actualizando fuente de agua ID {}", waterSourceId);
        return waterSourceRepository.save(waterSource);
    }

    @Transactional
    public void deleteWaterSource(Integer waterSourceId) {
        WaterSource waterSource = getWaterSourceById(waterSourceId);

        // La entidad WaterSource tiene cascade = CascadeType.ALL para reservoirTurns.
        // Esto significa que JPA eliminará automáticamente los ReservoirTurn asociados.
        // Si se necesitaran validaciones antes de eliminar (ej. no borrar si tiene turnos activos),
        // se harían aquí antes de llamar a delete.
        // Ejemplo:
        // if (reservoirTurnRepository.existsByWaterSourceAndIsActive(waterSource, true)) { // Método hipotético
        //     throw new IllegalStateException("No se puede eliminar la fuente de agua porque tiene turnos de embalse activos.");
        // }

        log.warn("Eliminando fuente de agua ID {}", waterSourceId);
        waterSourceRepository.delete(waterSource);
    }
}