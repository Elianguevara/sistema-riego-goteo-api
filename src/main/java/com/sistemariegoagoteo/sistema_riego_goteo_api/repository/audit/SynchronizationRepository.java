package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.Synchronization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Synchronization.
 */
@Repository
public interface SynchronizationRepository extends JpaRepository<Synchronization, Integer> {
    List<Synchronization> findByIsSynchronized(Boolean isSynchronized);
    List<Synchronization> findByModifiedTableAndIsSynchronized(String modifiedTable, Boolean isSynchronized);
}