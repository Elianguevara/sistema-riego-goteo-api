package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repositorio para la entidad ChangeHistory.
 */
@Repository
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Integer> {
    List<ChangeHistory> findByUser(User user);
    List<ChangeHistory> findByAffectedTable(String affectedTable);
    List<ChangeHistory> findByChangeDatetimeBetween(Date startDate, Date endDate);
}