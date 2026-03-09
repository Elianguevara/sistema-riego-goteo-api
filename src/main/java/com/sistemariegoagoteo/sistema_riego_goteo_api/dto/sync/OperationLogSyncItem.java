package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Item individual de sincronización de bitácora de operaciones desde el móvil.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogSyncItem {

    @NotNull(message = "El ID local del móvil es requerido.")
    private String localId;

    @NotNull(message = "El ID de la finca es requerido.")
    private Integer farmId;

    @NotNull(message = "La fecha y hora de la operación son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime operationDatetime;

    @NotNull(message = "El tipo de operación es requerido.")
    private String operationType;

    private String description;
}
