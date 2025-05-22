package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Para queries dinámicas
import org.springframework.stereotype.Repository;

import java.util.Date;


@Repository
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Integer>, JpaSpecificationExecutor<ChangeHistory> {

    // Métodos para búsquedas comunes (puedes usar JpaSpecificationExecutor para más flexibilidad)
    Page<ChangeHistory> findByUserOrderByChangeDatetimeDesc(User user, Pageable pageable);
    Page<ChangeHistory> findByAffectedTableOrderByChangeDatetimeDesc(String affectedTable, Pageable pageable);
    Page<ChangeHistory> findByAffectedTableAndUserOrderByChangeDatetimeDesc(String affectedTable, User user, Pageable pageable);
    Page<ChangeHistory> findByChangeDatetimeBetweenOrderByChangeDatetimeDesc(Date startDate, Date endDate, Pageable pageable);

    // Ejemplo con Like para búsqueda en campos de texto (ignora mayúsculas/minúsculas)
    Page<ChangeHistory> findByOldValueContainingIgnoreCaseOrNewValueContainingIgnoreCaseOrderByChangeDatetimeDesc(
            String oldValueSearch, String newValueSearch, Pageable pageable);
}