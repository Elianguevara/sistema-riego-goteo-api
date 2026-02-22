package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.report.ReportTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportTaskRepository extends JpaRepository<ReportTask, UUID> {
}
