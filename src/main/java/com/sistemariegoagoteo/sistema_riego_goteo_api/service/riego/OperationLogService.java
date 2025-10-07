package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.OperationLogRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.OperationLog;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.OperationLogRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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

        OperationLog operationLog = new OperationLog();
        operationLog.setFarm(farm);
        operationLog.setOperationDatetime(request.getOperationDatetime()); // <-- CAMBIO DE NOMBRE
        operationLog.setOperationType(request.getOperationType());         // <-- LÍNEA AÑADIDA
        operationLog.setDescription(request.getDescription());

        OperationLog savedLog = operationLogRepository.save(operationLog);
        auditService.logChange(currentUser, "CREATE", OperationLog.class.getSimpleName(), "id", null, savedLog.getId().toString());

        log.info("Registrando bitácora (tipo: {}) para finca ID {}", request.getOperationType(), farmId);
        return savedLog;
    }

    @Transactional
    public OperationLog updateOperationLog(Integer logId, OperationLogRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        OperationLog operationLog = getOperationLogById(logId);

        // Auditoría
        if (!Objects.equals(operationLog.getOperationDatetime(), request.getOperationDatetime())) {
            auditService.logChange(currentUser, "UPDATE", OperationLog.class.getSimpleName(), "operationDatetime", Objects.toString(operationLog.getOperationDatetime(), null), Objects.toString(request.getOperationDatetime(), null));
        }
        if (!Objects.equals(operationLog.getOperationType(), request.getOperationType())) {
            auditService.logChange(currentUser, "UPDATE", OperationLog.class.getSimpleName(), "operationType", operationLog.getOperationType(), request.getOperationType());
        }
        if (!Objects.equals(operationLog.getDescription(), request.getDescription())) {
            auditService.logChange(currentUser, "UPDATE", OperationLog.class.getSimpleName(), "description", operationLog.getDescription(), request.getDescription());
        }

        operationLog.setOperationDatetime(request.getOperationDatetime()); // <-- CAMBIO DE NOMBRE
        operationLog.setOperationType(request.getOperationType());         // <-- LÍNEA AÑADIDA
        operationLog.setDescription(request.getDescription());

        log.info("Actualizando bitácora de operación ID {}", logId);
        return operationLogRepository.save(operationLog);
    }

    // Los métodos GET y DELETE no necesitan cambios en su lógica interna
    @Transactional
    public void deleteOperationLog(Integer logId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        OperationLog operationLog = getOperationLogById(logId);
        auditService.logChange(currentUser, "DELETE", OperationLog.class.getSimpleName(), "id", operationLog.getId().toString(), null);
        log.warn("Eliminando bitácora de operación ID {}", logId);
        operationLogRepository.delete(operationLog);
    }

    @Transactional(readOnly = true)
    public List<OperationLog> getOperationLogsByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        return operationLogRepository.findByFarmOrderByOperationDatetimeDesc(farm); // Ordenar por el nuevo campo
    }

    @Transactional(readOnly = true)
    public OperationLog getOperationLogById(Integer logId) {
        return operationLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("OperationLog", "id", logId));
    }
}