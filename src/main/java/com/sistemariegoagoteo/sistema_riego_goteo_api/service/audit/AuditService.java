package com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.Synchronization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit.ChangeHistoryRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit.SynchronizationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final ChangeHistoryRepository changeHistoryRepository;
    private final SynchronizationRepository synchronizationRepository;
    private final UserRepository userRepository;

    /**
     * Guarda un registro de auditoría detallado.
     * @param user El usuario que realiza la acción.
     * @param actionType El tipo de acción (CREATE, UPDATE, DELETE).
     * @param affectedTable La tabla afectada.
     * @param changedField El campo que cambió (o el ID para deletes).
     * @param oldValue El valor antiguo del campo.
     * @param newValue El valor nuevo del campo.
     */
    @Transactional
    public void logChange(User user, String actionType, String affectedTable, String changedField, String oldValue, String newValue) {
        ChangeHistory logEntry = new ChangeHistory();
        logEntry.setUser(user);
        logEntry.setActionType(actionType); // Se guarda el tipo de acción
        logEntry.setAffectedTable(affectedTable);
        logEntry.setChangedField(changedField);
        logEntry.setOldValue(oldValue);
        logEntry.setNewValue(newValue);
        logEntry.setChangeDatetime(new Date());
        changeHistoryRepository.save(logEntry);
        log.debug("Change logged: User '{}' performed {} on table '{}', field '{}'", user.getUsername(), actionType, affectedTable, changedField);
    }

    @Transactional(readOnly = true)
    public Page<ChangeHistory> getChangeHistory(
            Long userId, String affectedTable, String actionType, String searchTerm,
            Date startDate, Date endDate, Pageable pageable) {
        
        Specification<ChangeHistory> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                predicates.add(criteriaBuilder.equal(root.get("user"), user));
            }
            if (affectedTable != null && !affectedTable.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("affectedTable"), affectedTable));
            }
            // --- NUEVA LÓGICA DE FILTRADO POR ACCIÓN ---
            if (actionType != null && !actionType.isEmpty()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("actionType")), actionType.toLowerCase()));
            }
            // ------------------------------------------
            if (searchTerm != null && !searchTerm.isEmpty()) {
                Predicate oldValuePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("oldValue")), "%" + searchTerm.toLowerCase() + "%");
                Predicate newValuePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("newValue")), "%" + searchTerm.toLowerCase() + "%");
                Predicate changedFieldPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("changedField")), "%" + searchTerm.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(oldValuePredicate, newValuePredicate, changedFieldPredicate));
            }
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("changeDatetime"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("changeDatetime"), endDate));
            }
            if (pageable.getSort().isUnsorted() && query != null) {
                 query.orderBy(criteriaBuilder.desc(root.get("changeDatetime")));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return changeHistoryRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<ChangeHistory> getChangeHistoryDetail(Integer logId) {
        return changeHistoryRepository.findById(logId);
    }

    // --- Métodos de Synchronization (sin cambios) ---
    
    @Transactional
    public void recordModificationForSync(String tableName, Integer recordId) {
        Synchronization syncRecord = synchronizationRepository
                .findByModifiedTableAndModifiedRecordId(tableName, recordId)
                .orElse(new Synchronization());

        syncRecord.setModifiedTable(tableName);
        syncRecord.setModifiedRecordId(recordId);
        syncRecord.setModificationDatetime(new Date());
        syncRecord.setIsSynchronized(false);

        synchronizationRepository.save(syncRecord);
        log.info("Recorded modification for sync: Table '{}', Record ID '{}'", tableName, recordId);
    }

    @Transactional(readOnly = true)
    public Page<Synchronization> getPendingSynchronizations(String tableName, Pageable pageable) {
        if (tableName != null && !tableName.isEmpty()) {
            return synchronizationRepository.findByModifiedTableAndIsSynchronized(tableName, false, pageable);
        } else {
            return synchronizationRepository.findByIsSynchronized(false, pageable);
        }
    }
    
    @Transactional(readOnly = true)
    public Page<Synchronization> getAllSynchronizationRecords(String tableName, Boolean isSynchronized, Pageable pageable) {
        Specification<Synchronization> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tableName != null && !tableName.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("modifiedTable"), tableName));
            }
            if (isSynchronized != null) {
                predicates.add(criteriaBuilder.equal(root.get("isSynchronized"), isSynchronized));
            }
            if (pageable.getSort().isUnsorted() && query != null) {
                 query.orderBy(criteriaBuilder.desc(root.get("modificationDatetime")));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return synchronizationRepository.findAll(spec, pageable);
    }

    @Transactional
    public Synchronization updateSynchronizationStatus(Integer syncId, boolean synchronizedStatus) {
        Synchronization syncRecord = synchronizationRepository.findById(syncId)
                .orElseThrow(() -> new ResourceNotFoundException("Synchronization", "id", syncId));

        syncRecord.setIsSynchronized(synchronizedStatus);
        syncRecord.setModificationDatetime(new Date());
        log.info("Updating synchronization status for sync ID {}: {}", syncId, synchronizedStatus);
        return synchronizationRepository.save(syncRecord);
    }

    @Transactional
    public int batchUpdateSynchronizationStatus(List<Integer> syncIds, boolean synchronizedStatus) {
        if (syncIds == null || syncIds.isEmpty()) {
            return 0;
        }
        log.info("Batch updating synchronization status for {} IDs to: {}", syncIds.size(), synchronizedStatus);
        return synchronizationRepository.updateSynchronizationStatusForIds(syncIds, synchronizedStatus, new Date());
    }
}