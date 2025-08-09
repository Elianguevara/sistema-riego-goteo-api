package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskRequest {

    @NotBlank(message = "La descripción de la tarea no puede estar vacía.")
    @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres.")
    private String description;

    @NotNull(message = "Debe especificar el ID del operario asignado.")
    private Long assignedToUserId;
}