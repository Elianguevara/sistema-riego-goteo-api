package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

@Data
public class OperationLogRequest {

    @NotNull(message = "La fecha y hora de la operación son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date operationDatetime; // <-- CAMBIO DE NOMBRE

    // --- CAMPO AÑADIDO (SUGERENCIA) ---
    @NotBlank(message = "El tipo de operación no puede estar vacío.")
    @Size(max = 100, message = "El tipo de operación es demasiado largo.")
    private String operationType;

    @Size(max = 65535, message = "La descripción es demasiado larga.")
    private String description;
}