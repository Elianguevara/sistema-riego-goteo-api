package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para la respuesta con información de una finca.
 */
@Data
@NoArgsConstructor
public class FarmResponse {
    /**
     * Identificador único de la finca.
     */
    private Integer id;

    /**
     * Nombre de la finca.
     */
    private String name;

    /**
     * Ubicación de la finca.
     */
    private String location;

    /**
     * Capacidad del reservorio en metros cúbicos.
     */
    private BigDecimal reservoirCapacity;

    /**
     * Tamaño de la finca en hectáreas.
     */
    private BigDecimal farmSize;

    /**
     * Coordenada de latitud.
     */
    private BigDecimal latitude;

    /**
     * Coordenada de longitud.
     */
    private BigDecimal longitude;

    public FarmResponse(Farm farm) {
        this.id = farm.getId();
        this.name = farm.getName();
        this.location = farm.getLocation();
        this.reservoirCapacity = farm.getReservoirCapacity();
        this.farmSize = farm.getFarmSize();
        // --- LÓGICA DE MAPEO AÑADIDA ---
        this.latitude = farm.getLatitude();
        this.longitude = farm.getLongitude();
    }
}