package com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit.ChangeHistoryRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository; // Para buscar usuario por ID
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;

// Imports para JpaSpecificationExecutor
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
    private final UserRepository userRepository; // Para buscar usuario por ID

    // Método interno para registrar cambios (no expuesto directamente vía API para creación)
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
            // Ordenar por fecha descendente por defecto si no se especifica en Pageable
            if (pageable.getSort().isUnsorted()) {
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
}