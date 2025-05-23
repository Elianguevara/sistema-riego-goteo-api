package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IrrigationSyncResultItem {
    private String localId; // El ID local que envió el móvil
    private Integer serverId; // El ID asignado por el servidor si fue exitoso
    private boolean success;
    private String message; // Mensaje de error o éxito
}