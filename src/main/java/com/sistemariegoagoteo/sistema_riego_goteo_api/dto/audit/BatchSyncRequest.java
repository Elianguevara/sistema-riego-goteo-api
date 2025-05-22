package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BatchSyncRequest {
    @NotEmpty(message = "La lista de IDs de sincronización no puede estar vacía.")
    private List<Integer> syncIds;

    @NotNull(message = "El estado de sincronización es requerido.")
    private Boolean isSynchronized;
}