package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SynchronizationStatusUpdateRequest {
    @NotNull(message = "El estado de sincronización es requerido (true/false).")
    private Boolean isSynchronized;
}