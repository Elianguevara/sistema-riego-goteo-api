package com.sistemariegoagoteo.sistema_riego_goteo_api.service.sync;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio optimizado para la sincronización de datos desde dispositivos
 * móviles. Implementa procesamiento por lotes para minimizar las transacciones
 * de base de datos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileSyncService {

    private final IrrigationRepository irrigationRepository;
    private final FertilizationRepository fertilizationRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final OperationLogRepository operationLogRepository;
    private final TaskRepository taskRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository;
    private final AuditService auditService;

    private static final BigDecimal METERS_CUBIC_TO_HECTOLITERS = new BigDecimal("10");

    // ═══════════════════════════════════════════════════════════════════════════
    // RIEGO (existente, sin cambios)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public IrrigationSyncResponse processIrrigationBatch(String username, IrrigationSyncBatchRequest batchRequest) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<IrrigationSyncItem> items = batchRequest.getIrrigations();
        List<IrrigationSyncResultItem> results = new ArrayList<>();

        if (items.isEmpty()) {
            return new IrrigationSyncResponse(0, 0, 0, results);
        }

        Set<Integer> sectorIds = items.stream().map(IrrigationSyncItem::getSectorId).collect(Collectors.toSet());
        Set<Integer> equipmentIds = items.stream().map(IrrigationSyncItem::getEquipmentId).collect(Collectors.toSet());

        Map<Integer, Sector> sectorMap = sectorRepository.findAllById(sectorIds).stream()
                .collect(Collectors.toMap(Sector::getId, Function.identity()));
        Map<Integer, IrrigationEquipment> equipmentMap = equipmentRepository.findAllById(equipmentIds).stream()
                .collect(Collectors.toMap(IrrigationEquipment::getId, Function.identity()));

        List<Irrigation> entitiesToSave = new ArrayList<>();
        Map<String, IrrigationSyncResultItem> resultMap = new HashMap<>();
        Map<String, String> actionMap = new HashMap<>();

        for (IrrigationSyncItem item : items) {
            IrrigationSyncResultItem resultItem = new IrrigationSyncResultItem();
            resultItem.setLocalId(item.getLocalId());
            try {
                Sector sector = sectorMap.get(item.getSectorId());
                if (sector == null) throw new ResourceNotFoundException("Sector", "id", item.getSectorId());

                IrrigationEquipment equipment = equipmentMap.get(item.getEquipmentId());
                if (equipment == null) throw new ResourceNotFoundException("IrrigationEquipment", "id", item.getEquipmentId());

                if (!equipment.getFarm().getId().equals(sector.getFarm().getId())) {
                    throw new IllegalArgumentException("El equipo " + equipment.getId() + " no pertenece a la finca del sector.");
                }

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
                actionMap.put(item.getLocalId(), action);

                irrigation.setSector(sector);
                irrigation.setEquipment(equipment);
                irrigation.setStartDatetime(item.getStartDatetime());
                irrigation.setEndDatetime(item.getEndDatetime());
                irrigation.setIrrigationHours(calculateIrrigationHours(item.getStartDatetime(), item.getEndDatetime()));
                irrigation.setWaterAmount(calculateWaterAmount(equipment.getMeasuredFlow(), irrigation.getIrrigationHours()));

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

        List<Irrigation> savedEntities = new ArrayList<>();
        if (!entitiesToSave.isEmpty()) {
            savedEntities = irrigationRepository.saveAll(entitiesToSave);
            log.info("Se han guardado/actualizado {} registros de riego en lote.", savedEntities.size());
        }

        int successfulItems = 0;
        int failedItems = items.size() - savedEntities.size();
        for (Irrigation saved : savedEntities) {
            IrrigationSyncResultItem res = resultMap.get(saved.getLocalMobileId());
            if (res != null) {
                res.setServerId(saved.getId());
                successfulItems++;
                String action = actionMap.getOrDefault(saved.getLocalMobileId(), "UPDATE");
                String oldValue = "UPDATE".equals(action) ? saved.getId().toString() : null;
                auditService.logChange(currentUser, "SYNC_" + action, Irrigation.class.getSimpleName(),
                        "id", oldValue, saved.getId().toString());
            }
        }

        return new IrrigationSyncResponse(items.size(), successfulItems, failedItems, new ArrayList<>(resultMap.values()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FERTILIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public MobileSyncResponse processFertilizationBatch(String username, FertilizationSyncBatchRequest batchRequest) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<FertilizationSyncItem> items = batchRequest.getFertilizations();
        if (items.isEmpty()) return new MobileSyncResponse(0, 0, 0, List.of());

        // Bulk fetch sectors
        Set<Integer> sectorIds = items.stream().map(FertilizationSyncItem::getSectorId).collect(Collectors.toSet());
        Map<Integer, Sector> sectorMap = sectorRepository.findAllById(sectorIds).stream()
                .collect(Collectors.toMap(Sector::getId, Function.identity()));

        List<Fertilization> entitiesToSave = new ArrayList<>();
        Map<String, MobileSyncResultItem> resultMap = new LinkedHashMap<>();
        Map<String, String> actionMap = new HashMap<>();

        for (FertilizationSyncItem item : items) {
            MobileSyncResultItem resultItem = new MobileSyncResultItem();
            resultItem.setLocalId(item.getLocalId());
            try {
                Sector sector = sectorMap.get(item.getSectorId());
                if (sector == null) throw new ResourceNotFoundException("Sector", "id", item.getSectorId());

                UnitOfMeasure unit = UnitOfMeasure.valueOf(item.getQuantityUnit().toUpperCase());

                Optional<Fertilization> existingOpt = fertilizationRepository.findByLocalMobileId(item.getLocalId());
                Fertilization fertilization;
                String action;
                if (existingOpt.isPresent()) {
                    fertilization = existingOpt.get();
                    action = "UPDATE";
                } else {
                    fertilization = new Fertilization();
                    fertilization.setLocalMobileId(item.getLocalId());
                    action = "CREATE";
                }
                actionMap.put(item.getLocalId(), action);

                fertilization.setSector(sector);
                fertilization.setDate(toDate(item.getDate()));
                fertilization.setFertilizerType(item.getFertilizerType());
                fertilization.setQuantity(item.getQuantity());
                fertilization.setQuantityUnit(unit);

                entitiesToSave.add(fertilization);
                resultItem.setSuccess(true);
                resultItem.setMessage(action + " procesado correctamente.");
            } catch (Exception e) {
                log.error("Error procesando fertilización móvil {}: {}", item.getLocalId(), e.getMessage());
                resultItem.setSuccess(false);
                resultItem.setMessage("Error: " + e.getMessage());
            }
            resultMap.put(item.getLocalId(), resultItem);
        }

        List<Fertilization> saved = new ArrayList<>();
        if (!entitiesToSave.isEmpty()) {
            saved = fertilizationRepository.saveAll(entitiesToSave);
            log.info("Se han guardado/actualizado {} registros de fertilización en lote.", saved.size());
        }

        int successfulItems = 0;
        for (Fertilization s : saved) {
            MobileSyncResultItem res = resultMap.get(s.getLocalMobileId());
            if (res != null) {
                res.setServerId(s.getId());
                successfulItems++;
                String action = actionMap.getOrDefault(s.getLocalMobileId(), "UPDATE");
                auditService.logChange(currentUser, "SYNC_" + action, Fertilization.class.getSimpleName(),
                        "id", "UPDATE".equals(action) ? s.getId().toString() : null, s.getId().toString());
            }
        }

        int failedItems = items.size() - saved.size();
        return new MobileSyncResponse(items.size(), successfulItems, failedItems, new ArrayList<>(resultMap.values()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MANTENIMIENTO
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public MobileSyncResponse processMaintenanceBatch(String username, MaintenanceSyncBatchRequest batchRequest) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<MaintenanceSyncItem> items = batchRequest.getMaintenances();
        if (items.isEmpty()) return new MobileSyncResponse(0, 0, 0, List.of());

        // Bulk fetch equipments
        Set<Integer> equipmentIds = items.stream().map(MaintenanceSyncItem::getEquipmentId).collect(Collectors.toSet());
        Map<Integer, IrrigationEquipment> equipmentMap = equipmentRepository.findAllById(equipmentIds).stream()
                .collect(Collectors.toMap(IrrigationEquipment::getId, Function.identity()));

        List<Maintenance> entitiesToSave = new ArrayList<>();
        Map<String, MobileSyncResultItem> resultMap = new LinkedHashMap<>();
        Map<String, String> actionMap = new HashMap<>();

        for (MaintenanceSyncItem item : items) {
            MobileSyncResultItem resultItem = new MobileSyncResultItem();
            resultItem.setLocalId(item.getLocalId());
            try {
                IrrigationEquipment equipment = equipmentMap.get(item.getEquipmentId());
                if (equipment == null) throw new ResourceNotFoundException("IrrigationEquipment", "id", item.getEquipmentId());

                Optional<Maintenance> existingOpt = maintenanceRepository.findByLocalMobileId(item.getLocalId());
                Maintenance maintenance;
                String action;
                if (existingOpt.isPresent()) {
                    maintenance = existingOpt.get();
                    action = "UPDATE";
                } else {
                    maintenance = new Maintenance();
                    maintenance.setLocalMobileId(item.getLocalId());
                    action = "CREATE";
                }
                actionMap.put(item.getLocalId(), action);

                maintenance.setIrrigationEquipment(equipment);
                maintenance.setDate(toDate(item.getDate()));
                maintenance.setDescription(item.getDescription());
                maintenance.setWorkHours(item.getWorkHours());

                entitiesToSave.add(maintenance);
                resultItem.setSuccess(true);
                resultItem.setMessage(action + " procesado correctamente.");
            } catch (Exception e) {
                log.error("Error procesando mantenimiento móvil {}: {}", item.getLocalId(), e.getMessage());
                resultItem.setSuccess(false);
                resultItem.setMessage("Error: " + e.getMessage());
            }
            resultMap.put(item.getLocalId(), resultItem);
        }

        List<Maintenance> saved = new ArrayList<>();
        if (!entitiesToSave.isEmpty()) {
            saved = maintenanceRepository.saveAll(entitiesToSave);
            log.info("Se han guardado/actualizado {} registros de mantenimiento en lote.", saved.size());
        }

        int successfulItems = 0;
        for (Maintenance s : saved) {
            MobileSyncResultItem res = resultMap.get(s.getLocalMobileId());
            if (res != null) {
                res.setServerId(s.getId());
                successfulItems++;
                String action = actionMap.getOrDefault(s.getLocalMobileId(), "UPDATE");
                auditService.logChange(currentUser, "SYNC_" + action, Maintenance.class.getSimpleName(),
                        "id", "UPDATE".equals(action) ? s.getId().toString() : null, s.getId().toString());
            }
        }

        int failedItems = items.size() - saved.size();
        return new MobileSyncResponse(items.size(), successfulItems, failedItems, new ArrayList<>(resultMap.values()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BITÁCORA DE OPERACIONES
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public MobileSyncResponse processOperationLogBatch(String username, OperationLogSyncBatchRequest batchRequest) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<OperationLogSyncItem> items = batchRequest.getOperationLogs();
        if (items.isEmpty()) return new MobileSyncResponse(0, 0, 0, List.of());

        // Bulk fetch farms
        Set<Integer> farmIds = items.stream().map(OperationLogSyncItem::getFarmId).collect(Collectors.toSet());
        Map<Integer, Farm> farmMap = farmRepository.findAllById(farmIds).stream()
                .collect(Collectors.toMap(Farm::getId, Function.identity()));

        List<OperationLog> entitiesToSave = new ArrayList<>();
        Map<String, MobileSyncResultItem> resultMap = new LinkedHashMap<>();
        Map<String, String> actionMap = new HashMap<>();

        for (OperationLogSyncItem item : items) {
            MobileSyncResultItem resultItem = new MobileSyncResultItem();
            resultItem.setLocalId(item.getLocalId());
            try {
                Farm farm = farmMap.get(item.getFarmId());
                if (farm == null) throw new ResourceNotFoundException("Farm", "id", item.getFarmId());

                Optional<OperationLog> existingOpt = operationLogRepository.findByLocalMobileId(item.getLocalId());
                OperationLog operationLog;
                String action;
                if (existingOpt.isPresent()) {
                    operationLog = existingOpt.get();
                    action = "UPDATE";
                } else {
                    operationLog = new OperationLog();
                    operationLog.setLocalMobileId(item.getLocalId());
                    action = "CREATE";
                }
                actionMap.put(item.getLocalId(), action);

                operationLog.setFarm(farm);
                operationLog.setOperationDatetime(toDate(item.getOperationDatetime()));
                operationLog.setOperationType(item.getOperationType());
                operationLog.setDescription(item.getDescription());

                entitiesToSave.add(operationLog);
                resultItem.setSuccess(true);
                resultItem.setMessage(action + " procesado correctamente.");
            } catch (Exception e) {
                log.error("Error procesando bitácora móvil {}: {}", item.getLocalId(), e.getMessage());
                resultItem.setSuccess(false);
                resultItem.setMessage("Error: " + e.getMessage());
            }
            resultMap.put(item.getLocalId(), resultItem);
        }

        List<OperationLog> saved = new ArrayList<>();
        if (!entitiesToSave.isEmpty()) {
            saved = operationLogRepository.saveAll(entitiesToSave);
            log.info("Se han guardado/actualizado {} entradas de bitácora en lote.", saved.size());
        }

        int successfulItems = 0;
        for (OperationLog s : saved) {
            MobileSyncResultItem res = resultMap.get(s.getLocalMobileId());
            if (res != null) {
                res.setServerId(s.getId());
                successfulItems++;
                String action = actionMap.getOrDefault(s.getLocalMobileId(), "UPDATE");
                auditService.logChange(currentUser, "SYNC_" + action, OperationLog.class.getSimpleName(),
                        "id", "UPDATE".equals(action) ? s.getId().toString() : null, s.getId().toString());
            }
        }

        int failedItems = items.size() - saved.size();
        return new MobileSyncResponse(items.size(), successfulItems, failedItems, new ArrayList<>(resultMap.values()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTADO DE TAREAS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public MobileSyncResponse processTaskStatusBatch(String username, TaskStatusSyncBatchRequest batchRequest) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<TaskStatusSyncItem> items = batchRequest.getTaskStatusUpdates();
        if (items.isEmpty()) return new MobileSyncResponse(0, 0, 0, List.of());

        // Bulk fetch tasks
        Set<Long> taskIds = items.stream().map(TaskStatusSyncItem::getTaskId).collect(Collectors.toSet());
        Map<Long, Task> taskMap = taskRepository.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        Map<String, MobileSyncResultItem> resultMap = new LinkedHashMap<>();
        List<Task> tasksToSave = new ArrayList<>();

        for (TaskStatusSyncItem item : items) {
            MobileSyncResultItem resultItem = new MobileSyncResultItem();
            resultItem.setLocalId(item.getLocalId());
            try {
                Task task = taskMap.get(item.getTaskId());
                if (task == null) throw new ResourceNotFoundException("Task", "id", item.getTaskId());

                if (!task.getAssignedTo().getId().equals(currentUser.getId())) {
                    throw new SecurityException("No tienes permiso para actualizar la tarea " + item.getTaskId());
                }

                TaskStatus newStatus = TaskStatus.valueOf(item.getNewStatus().toUpperCase());
                task.setStatus(newStatus);

                tasksToSave.add(task);
                resultItem.setSuccess(true);
                resultItem.setMessage("Estado actualizado a " + newStatus.name());
                resultItem.setServerId(task.getId().intValue());
            } catch (Exception e) {
                log.error("Error procesando estado de tarea móvil {}: {}", item.getLocalId(), e.getMessage());
                resultItem.setSuccess(false);
                resultItem.setMessage("Error: " + e.getMessage());
            }
            resultMap.put(item.getLocalId(), resultItem);
        }

        if (!tasksToSave.isEmpty()) {
            taskRepository.saveAll(tasksToSave);
            log.info("Se han actualizado {} estados de tareas en lote.", tasksToSave.size());
        }

        int successfulItems = (int) resultMap.values().stream().filter(MobileSyncResultItem::isSuccess).count();
        int failedItems = items.size() - successfulItems;

        tasksToSave.forEach(t ->
                auditService.logChange(currentUser, "SYNC_UPDATE", Task.class.getSimpleName(),
                        "status", null, t.getStatus().name()));

        return new MobileSyncResponse(items.size(), successfulItems, failedItems, new ArrayList<>(resultMap.values()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

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
        return flowRate.multiply(hours).multiply(METERS_CUBIC_TO_HECTOLITERS).setScale(2, RoundingMode.HALF_UP);
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
