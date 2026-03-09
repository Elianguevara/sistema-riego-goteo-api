package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado individual para un item dentro de cualquier respuesta de
 * sincronización móvil.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileSyncResultItem {
    private String localId;     // UUID generado por el móvil
    private Integer serverId;   // ID asignado por el servidor (null si falló)
    private boolean success;
    private String message;
}
