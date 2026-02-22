package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para la solicitud de creación o actualización de una finca.
 */
@Data
public class FarmRequest {
    /**
     * Nombre de la finca.
     */
    @NotBlank(message = "El nombre de la finca no puede estar vacío.")
    @Size(max = 100, message = "El nombre de la finca no puede exceder los 100 caracteres.")
    private String name;

    /**
     * Ubicación geográfica o dirección de la finca.
     */
    @Size(max = 255, message = "La ubicación no puede exceder los 255 caracteres.")
    private String location;

    /**
     * Capacidad del reservorio en metros cúbicos.
     */
    @NotNull(message = "La capacidad del reservorio es requerida.")
    @PositiveOrZero(message = "La capacidad del reservorio debe ser un valor positivo o cero.")
    private BigDecimal reservoirCapacity;

    /**
     * Tamaño de la finca en hectáreas.
     */
    @NotNull(message = "El tamaño de la finca es requerido.")
    @PositiveOrZero(message = "El tamaño de la finca debe ser un valor positivo o cero.")
    private BigDecimal farmSize;

    // --- CAMPOS NUEVOS AÑADIDOS ---
    /**
     * Coordenada de latitud (opcional, se puede obtener por geocodificación).
     */
    private BigDecimal latitude;

    /**
     * Coordenada de longitud (opcional, se puede obtener por geocodificación).
     */
    private BigDecimal longitude;
}