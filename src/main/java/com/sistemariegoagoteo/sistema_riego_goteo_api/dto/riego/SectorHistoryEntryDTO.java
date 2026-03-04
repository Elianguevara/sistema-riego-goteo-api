package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entrada unificada del historial de actividades de un sector.
 * Puede representar un riego, una fertilización o un mantenimiento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectorHistoryEntryDTO {

    /** Tipo de actividad: RIEGO, FERTILIZACION o MANTENIMIENTO */
    private String type;

    /** ID del registro original */
    private Integer id;

    /** Fecha y hora de la actividad */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;

    /** Descripción resumida de la actividad */
    private String description;

    /** Datos específicos del tipo de actividad */
    private Map<String, Object> details;
}
