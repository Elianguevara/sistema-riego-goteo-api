package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.OperationLogRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.OperationLog;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.OperationLogRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final FarmRepository farmRepository;
    private final AuditService auditService;

    @Transactional
    public OperationLog createOperationLog(Integer farmId, OperationLogRequest request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        if (request.getEndDatetime() != null && request.getEndDatetime().before(request.getStartDatetime())) {
            throw new IllegalArgumentException("La fecha de finalización de la operación no puede ser anterior a la fecha de inicio.");
        }

        OperationLog operationLog = new OperationLog();
        operationLog.setFarm(farm);
        operationLog.setStartDatetime(request.getStartDatetime());
        operationLog.setEndDatetime(request.getEndDatetime());

        log.info("Registrando bitácora de operación para finca ID {} desde {}",
                farmId, request.getStartDatetime());
        return operationLogRepository.save(operationLog);
    }

    @Transactional(readOnly = true)
    public List<OperationLog> getOperationLogsByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        // Asumiendo que OperationLogRepository tiene findByFarmOrderByStartDatetimeDesc
        return operationLogRepository.findByFarmOrderByStartDatetimeDesc(farm);
    }

    @Transactional(readOnly = true)
    public OperationLog getOperationLogById(Integer logId) {
        return operationLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("OperationLog", "id", logId));
    }

    @Transactional
    public OperationLog updateOperationLog(Integer logId, OperationLogRequest request) {
        OperationLog operationLog = getOperationLogById(logId); // Valida existencia

        // La finca de un log existente no debería cambiar.

        if (request.getEndDatetime() != null && request.getEndDatetime().before(request.getStartDatetime())) {
            throw new IllegalArgumentException("La fecha de finalización de la operación no puede ser anterior a la fecha de inicio.");
        }

        operationLog.setStartDatetime(request.getStartDatetime());
        operationLog.setEndDatetime(request.getEndDatetime());
        // Si se añadieran campos como 'description' o 'type', se actualizarían aquí.

        log.info("Actualizando bitácora de operación ID {}", logId);
        return operationLogRepository.save(operationLog);
    }

    @Transactional
    public void deleteOperationLog(Integer logId) {
        OperationLog operationLog = getOperationLogById(logId);
        log.warn("Eliminando bitácora de operación ID {}", logId);
        operationLogRepository.delete(operationLog);
    }
}