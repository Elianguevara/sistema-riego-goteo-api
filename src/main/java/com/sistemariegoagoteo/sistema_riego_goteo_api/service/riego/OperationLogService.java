package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.OperationLogRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.OperationLog;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.OperationLogRepository;
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
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final FarmRepository farmRepository;
    private final AuditService auditService;

    @Transactional
    public OperationLog createOperationLog(Integer farmId, OperationLogRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        if (request.getEndDatetime() != null && request.getEndDatetime().before(request.getStartDatetime())) {
            throw new IllegalArgumentException("La fecha de finalización de la operación no puede ser anterior a la fecha de inicio.");
        }

        OperationLog operationLog = new OperationLog();
        operationLog.setFarm(farm);
        operationLog.setStartDatetime(request.getStartDatetime());
        operationLog.setEndDatetime(request.getEndDatetime());

        OperationLog savedLog = operationLogRepository.save(operationLog);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", OperationLog.class.getSimpleName(), "id", null, savedLog.getId().toString());

        log.info("Registrando bitácora de operación para finca ID {} desde {}",
                farmId, request.getStartDatetime());
        return savedLog;
    }

    @Transactional
    public OperationLog updateOperationLog(Integer logId, OperationLogRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        OperationLog operationLog = getOperationLogById(logId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(operationLog.getStartDatetime(), request.getStartDatetime())) {
            auditService.logChange(currentUser, "UPDATE", OperationLog.class.getSimpleName(), "startDatetime", Objects.toString(operationLog.getStartDatetime(), null), Objects.toString(request.getStartDatetime(), null));
        }
        if (!Objects.equals(operationLog.getEndDatetime(), request.getEndDatetime())) {
            auditService.logChange(currentUser, "UPDATE", OperationLog.class.getSimpleName(), "endDatetime", Objects.toString(operationLog.getEndDatetime(), null), Objects.toString(request.getEndDatetime(), null));
        }

        if (request.getEndDatetime() != null && request.getEndDatetime().before(request.getStartDatetime())) {
            throw new IllegalArgumentException("La fecha de finalización de la operación no puede ser anterior a la fecha de inicio.");
        }

        operationLog.setStartDatetime(request.getStartDatetime());
        operationLog.setEndDatetime(request.getEndDatetime());

        log.info("Actualizando bitácora de operación ID {}", logId);
        return operationLogRepository.save(operationLog);
    }

    @Transactional
    public void deleteOperationLog(Integer logId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        OperationLog operationLog = getOperationLogById(logId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", OperationLog.class.getSimpleName(), "id", operationLog.getId().toString(), null);

        log.warn("Eliminando bitácora de operación ID {}", logId);
        operationLogRepository.delete(operationLog);
    }
    
    // --- MÉTODOS GET (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<OperationLog> getOperationLogsByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        return operationLogRepository.findByFarmOrderByStartDatetimeDesc(farm);
    }

    @Transactional(readOnly = true)
    public OperationLog getOperationLogById(Integer logId) {
        return operationLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("OperationLog", "id", logId));
    }
}