package com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit;

//import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.SynchronizationStatusUpdateRequest; // Asegúrate de que este DTO exista
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.Synchronization; // Añadir import
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit.ChangeHistoryRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit.SynchronizationRepository; // Añadir import
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
    private final SynchronizationRepository synchronizationRepository; // Inyectar nuevo repo
    private final UserRepository userRepository;

    // --- Métodos de ChangeHistory (existentes) ---
    @Transactional
    public void logChange(User user, String affectedTable, String changedField, String oldValue, String newValue) {
        ChangeHistory logEntry = new ChangeHistory();
        logEntry.setUser(user);
        logEntry.setAffectedTable(affectedTable);
        logEntry.setChangedField(changedField);
        logEntry.setOldValue(oldValue);
        logEntry.setNewValue(newValue);
        logEntry.setChangeDatetime(new Date());
        changeHistoryRepository.save(logEntry);
        log.debug("Change logged: User '{}' modified table '{}', field '{}'", user.getUsername(), affectedTable, changedField);
    }

    @Transactional(readOnly = true)
    public Page<ChangeHistory> getChangeHistory(
            Long userId, String affectedTable, String searchTerm,
            Date startDate, Date endDate, Pageable pageable) {
        // ... (implementación existente)
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


    // --- Nuevos Métodos para Synchronization ---

    /**
     * Registra o actualiza una entrada de sincronización cuando una entidad es modificada.
     * Este método sería llamado internamente por otros servicios o listeners.
     */
    @Transactional
    public void recordModificationForSync(String tableName, Integer recordId) {
        Synchronization syncRecord = synchronizationRepository
                .findByModifiedTableAndModifiedRecordId(tableName, recordId)
                .orElse(new Synchronization()); // Crea uno nuevo si no existe

        syncRecord.setModifiedTable(tableName);
        syncRecord.setModifiedRecordId(recordId);
        syncRecord.setModificationDatetime(new Date());
        syncRecord.setIsSynchronized(false); // Marcar como no sincronizado

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
        syncRecord.setModificationDatetime(new Date()); // Actualizar la fecha de la última acción sobre este registro de sync
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