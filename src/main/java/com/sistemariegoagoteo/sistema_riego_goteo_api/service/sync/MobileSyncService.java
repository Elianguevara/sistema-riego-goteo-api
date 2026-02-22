package com.sistemariegoagoteo.sistema_riego_goteo_api.service.sync;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncBatchRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncItem;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResultItem;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio optimizado para la sincronización de datos desde dispositivos
 * móviles.
 * Implementa procesamiento por lotes para minimizar las transacciones de base
 * de datos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileSyncService {

    private final IrrigationRepository irrigationRepository;
    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository;
    private final AuditService auditService;

    // Constantes para cálculos de precisión
    private static final BigDecimal METERS_CUBIC_TO_HECTOLITERS = new BigDecimal("10");

    /**
     * Procesa un lote de registros de riego enviados desde el móvil.
     * Utiliza optimización de consultas (Batch Fetching) para resolver el problema
     * N+1.
     */
    @Transactional
    public IrrigationSyncResponse processIrrigationBatch(String username, IrrigationSyncBatchRequest batchRequest) {
        // 1. Obtener usuario
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<IrrigationSyncItem> items = batchRequest.getIrrigations();
        List<IrrigationSyncResultItem> results = new ArrayList<>();

        if (items.isEmpty()) {
            return new IrrigationSyncResponse(0, 0, 0, results);
        }

        // 2. PRE-CARGA DE DATOS (Bulk Fetching)
        Set<Integer> sectorIds = items.stream().map(IrrigationSyncItem::getSectorId).collect(Collectors.toSet());
        Set<Integer> equipmentIds = items.stream().map(IrrigationSyncItem::getEquipmentId).collect(Collectors.toSet());

        // 3. MAPEO EN MEMORIA
        Map<Integer, Sector> sectorMap = sectorRepository.findAllById(sectorIds).stream()
                .collect(Collectors.toMap(Sector::getId, Function.identity()));

        Map<Integer, IrrigationEquipment> equipmentMap = equipmentRepository.findAllById(equipmentIds).stream()
                .collect(Collectors.toMap(IrrigationEquipment::getId, Function.identity()));

        List<Irrigation> entitiesToSave = new ArrayList<>();
        Map<String, IrrigationSyncResultItem> resultMap = new HashMap<>();

        // CORRECCIÓN: Usamos un mapa auxiliar para recordar la acción (CREATE/UPDATE)
        // de cada item
        Map<String, String> actionMap = new HashMap<>();

        // 4. PROCESAMIENTO EN MEMORIA
        for (IrrigationSyncItem item : items) {
            IrrigationSyncResultItem resultItem = new IrrigationSyncResultItem();
            resultItem.setLocalId(item.getLocalId());

            try {
                // Validación rápida
                Sector sector = sectorMap.get(item.getSectorId());
                if (sector == null) {
                    throw new ResourceNotFoundException("Sector", "id", item.getSectorId());
                }

                IrrigationEquipment equipment = equipmentMap.get(item.getEquipmentId());
                if (equipment == null) {
                    throw new ResourceNotFoundException("IrrigationEquipment", "id", item.getEquipmentId());
                }

                if (!equipment.getFarm().getId().equals(sector.getFarm().getId())) {
                    throw new IllegalArgumentException(
                            "El equipo " + equipment.getId() + " no pertenece a la finca del sector.");
                }

                // Buscar existencia
                Optional<Irrigation> existingOpt = irrigationRepository.findByLocalMobileId(item.getLocalId());

                Irrigation irrigation;
                String action;

                if (existingOpt.isPresent()) {
                    irrigation = existingOpt.get();
                    action = "UPDATE";
                } else {
                    irrigation = new Irrigation();
                    irrigation.setLocalMobileId(item.getLocalId());
                    action = "CREATE";
                }

                // Guardamos la acción en el mapa auxiliar para usarla después
                actionMap.put(item.getLocalId(), action);

                // Actualizar campos
                irrigation.setSector(sector);
                irrigation.setEquipment(equipment);
                irrigation.setStartDatetime(item.getStartDatetime());
                irrigation.setEndDatetime(item.getEndDatetime());

                // Cálculos precisos
                BigDecimal hours = calculateIrrigationHours(item.getStartDatetime(), item.getEndDatetime());
                BigDecimal water = calculateWaterAmount(equipment.getMeasuredFlow(), hours);

                irrigation.setIrrigationHours(hours);
                irrigation.setWaterAmount(water);

                entitiesToSave.add(irrigation);

                resultItem.setSuccess(true);
                resultItem.setMessage(action + " procesado correctamente.");
                resultItem.setServerId(null);

            } catch (Exception e) {
                log.error("Error procesando item móvil {}: {}", item.getLocalId(), e.getMessage());
                resultItem.setSuccess(false);
                resultItem.setMessage("Error: " + e.getMessage());
            }

            resultMap.put(item.getLocalId(), resultItem);
        }

        // 5. GUARDADO MASIVO (Batch Save)
        List<Irrigation> savedEntities = new ArrayList<>();
        if (!entitiesToSave.isEmpty()) {
            savedEntities = irrigationRepository.saveAll(entitiesToSave);
            log.info("Se han guardado/actualizado {} registros de riego en lote.", savedEntities.size());
        }

        // 6. POST-PROCESAMIENTO (Auditoría y IDs finales)
        int successfulItems = 0;
        int failedItems = items.size() - savedEntities.size();

        for (Irrigation saved : savedEntities) {
            IrrigationSyncResultItem res = resultMap.get(saved.getLocalMobileId());
            if (res != null) {
                res.setServerId(saved.getId());
                successfulItems++;

                // CORRECCIÓN: Recuperamos la acción del mapa en lugar de usar getCreatedAt()
                String action = actionMap.getOrDefault(saved.getLocalMobileId(), "UPDATE");

                // Si es UPDATE, el "valor anterior" (ID) es el mismo; si es CREATE, es null.
                String oldValue = "UPDATE".equals(action) ? saved.getId().toString() : null;

                auditService.logChange(currentUser, "SYNC_" + action, Irrigation.class.getSimpleName(),
                        "id", oldValue, saved.getId().toString());
            }
        }

        List<IrrigationSyncResultItem> finalResults = new ArrayList<>(resultMap.values());

        return new IrrigationSyncResponse(items.size(), successfulItems, failedItems, finalResults);
    }

    // --- Métodos de cálculo precisos ---

    private BigDecimal calculateIrrigationHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long diffInMinutes = java.time.Duration.between(start, end).toMinutes();
        return BigDecimal.valueOf(diffInMinutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWaterAmount(BigDecimal flowRate, BigDecimal hours) {
        if (flowRate == null || hours == null || flowRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal volumeM3 = flowRate.multiply(hours);
        return volumeM3.multiply(METERS_CUBIC_TO_HECTOLITERS).setScale(2, RoundingMode.HALF_UP);
    }
}