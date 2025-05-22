package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WaterSourceRequest {

    @NotBlank(message = "El tipo de fuente de agua no puede estar vacío.")
    @Size(max = 50, message = "El tipo de fuente de agua no puede exceder los 50 caracteres.")
    private String type; // Ej: Pozo, Represa, Canal, Red pública

    // farmId vendrá de la ruta del controlador
}