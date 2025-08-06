package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.WaterSourceRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.WaterSourceRepository;
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
public class WaterSourceService {

    private final WaterSourceRepository waterSourceRepository;
    private final FarmRepository farmRepository;
    private final AuditService auditService;

    @Transactional
    public WaterSource createWaterSource(Integer farmId, WaterSourceRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        waterSourceRepository.findByTypeAndFarm(request.getType(), farm).ifPresent(ws -> {
            throw new IllegalArgumentException("Ya existe una fuente de agua de tipo '" + request.getType() + "' en la finca '" + farm.getName() + "'.");
        });

        WaterSource waterSource = new WaterSource();
        waterSource.setFarm(farm);
        waterSource.setType(request.getType());

        WaterSource savedWaterSource = waterSourceRepository.save(waterSource);
        
        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", WaterSource.class.getSimpleName(), "type", null, savedWaterSource.getType());

        log.info("Creando fuente de agua tipo '{}' para finca ID {}", request.getType(), farmId);
        return savedWaterSource;
    }

    @Transactional
    public WaterSource updateWaterSource(Integer waterSourceId, WaterSourceRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WaterSource waterSource = getWaterSourceById(waterSourceId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(waterSource.getType(), request.getType())) {
            auditService.logChange(currentUser, "UPDATE", WaterSource.class.getSimpleName(), "type", waterSource.getType(), request.getType());
        }

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
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WaterSource waterSource = getWaterSourceById(waterSourceId);
        
        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", WaterSource.class.getSimpleName(), "id", waterSource.getId().toString(), null);

        log.warn("Eliminando fuente de agua ID {}", waterSourceId);
        waterSourceRepository.delete(waterSource);
    }

    // --- MÉTODOS GET (SIN CAMBIOS) ---
    
    @Transactional(readOnly = true)
    public List<WaterSource> getWaterSourcesByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        return waterSourceRepository.findByFarmOrderByTypeAsc(farm);
    }

    @Transactional(readOnly = true)
    public WaterSource getWaterSourceById(Integer waterSourceId) {
        return waterSourceRepository.findById(waterSourceId)
                .orElseThrow(() -> new ResourceNotFoundException("WaterSource", "id", waterSourceId));
    }
}