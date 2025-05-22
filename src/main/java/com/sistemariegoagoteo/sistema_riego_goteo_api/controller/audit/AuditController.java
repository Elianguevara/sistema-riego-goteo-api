package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.ChangeHistoryResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // Proteger todo el controlador para administradores
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/change-history")
    public ResponseEntity<Page<ChangeHistoryResponse>> getChangeHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String affectedTable,
            @RequestParam(required = false) String searchTerm, // Para buscar en oldValue, newValue, changedField
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @PageableDefault(size = 20, sort = "changeDatetime,desc") Pageable pageable) {

        log.info("Fetching change history with params: userId={}, affectedTable={}, searchTerm={}, startDate={}, endDate={}, pageable={}",
                userId, affectedTable, searchTerm, startDate, endDate, pageable);

        Page<ChangeHistory> historyPage = auditService.getChangeHistory(userId, affectedTable, searchTerm, startDate, endDate, pageable);
        Page<ChangeHistoryResponse> responsePage = historyPage.map(ChangeHistoryResponse::new);
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/change-history/{logId}")
    public ResponseEntity<ChangeHistoryResponse> getChangeHistoryDetail(@PathVariable Integer logId) {
        log.info("Fetching change history detail for log ID: {}", logId);
        ChangeHistory history = auditService.getChangeHistoryDetail(logId)
                .orElseThrow(() -> new ResourceNotFoundException("ChangeHistory", "id", logId));
        return ResponseEntity.ok(new ChangeHistoryResponse(history));
    }

    // NOTA: No se exponen endpoints POST/PUT/DELETE para ChangeHistory,
    // ya que estos logs deben ser inmutables y generados por el sistema.
}