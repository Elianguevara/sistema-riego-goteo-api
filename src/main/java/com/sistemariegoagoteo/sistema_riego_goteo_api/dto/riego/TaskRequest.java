package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskRequest {

    @NotBlank(message = "La descripción no puede estar vacía")
    private String description;

    @NotNull(message = "El ID del usuario asignado no puede ser nulo")
    private Long assignedToUserId;

    // --- CAMPO AÑADIDO Y OBLIGATORIO ---
    @NotNull(message = "El ID del sector no puede ser nulo")
    private Integer sectorId;
}