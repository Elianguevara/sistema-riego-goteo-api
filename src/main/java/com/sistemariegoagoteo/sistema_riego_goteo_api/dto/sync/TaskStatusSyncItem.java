package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Item individual de sincronización de cambio de estado de tarea desde el móvil.
 * El móvil ya conoce el taskId del servidor (lo obtuvo al cargar sus tareas),
 * por lo que no se necesita localMobileId en la entidad; se usa solo para
 * mapear el resultado de la respuesta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusSyncItem {

    /** UUID generado por el móvil para rastrear este resultado en la respuesta. */
    @NotNull(message = "El ID local del móvil es requerido.")
    private String localId;

    /** ID del servidor de la tarea a actualizar. */
    @NotNull(message = "El ID de la tarea es requerido.")
    private Long taskId;

    /**
     * Nuevo estado: "PENDIENTE", "EN_PROGRESO", "COMPLETADA" o "CANCELADA".
     */
    @NotNull(message = "El nuevo estado es requerido.")
    private String newStatus;
}
