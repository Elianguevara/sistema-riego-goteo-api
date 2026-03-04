package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta genérica para cualquier operación de sincronización móvil por lotes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileSyncResponse {
    private int totalItems;
    private int successfulItems;
    private int failedItems;
    private List<MobileSyncResultItem> results;
}
