package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SectorRequest {

    @NotBlank(message = "El nombre del sector no puede estar vacío.")
    @Size(max = 100, message = "El nombre del sector no puede exceder los 100 caracteres.")
    private String name;

    // farmId se manejará a través de la ruta en el controlador para la creación.
    // Para la actualización, generalmente no se cambia la finca de un sector directamente.

    private Integer equipmentId; // Opcional: ID del equipo de irrigación a asociar
}