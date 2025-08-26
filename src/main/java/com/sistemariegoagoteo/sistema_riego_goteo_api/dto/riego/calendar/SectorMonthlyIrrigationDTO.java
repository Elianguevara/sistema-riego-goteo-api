package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.calendar;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SectorMonthlyIrrigationDTO {
    private Integer sectorId;
    private String sectorName;
    // La clave es el día del mes (1-31), el valor es la lista de riegos de ese día.
    private Map<Integer, List<IrrigationCalendarEventDTO>> dailyIrrigations;
}