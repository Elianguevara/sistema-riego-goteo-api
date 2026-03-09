package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogSyncBatchRequest {

    @NotNull
    @NotEmpty(message = "La lista de entradas de bitácora no puede estar vacía.")
    private List<@Valid OperationLogSyncItem> operationLogs;
}
