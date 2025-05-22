package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.Synchronization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SynchronizationRepository extends JpaRepository<Synchronization, Integer>, JpaSpecificationExecutor<Synchronization> {

    Page<Synchronization> findByIsSynchronized(Boolean isSynchronized, Pageable pageable);

    Page<Synchronization> findByModifiedTableAndIsSynchronized(String modifiedTable, Boolean isSynchronized, Pageable pageable);

    Optional<Synchronization> findByModifiedTableAndModifiedRecordId(String modifiedTable, Integer modifiedRecordId);

    // Para actualización en lote
    @Modifying
    @Query("UPDATE Synchronization s SET s.isSynchronized = :status, s.modificationDatetime = :now WHERE s.id IN :ids")
    int updateSynchronizationStatusForIds(@Param("ids") List<Integer> ids, @Param("status") Boolean status, @Param("now") Date now);
}