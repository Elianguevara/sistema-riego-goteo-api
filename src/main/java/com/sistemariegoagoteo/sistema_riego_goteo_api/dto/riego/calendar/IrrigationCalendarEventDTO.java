package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.calendar;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class IrrigationCalendarEventDTO {
    private Integer irrigationId;
    private String equipmentName;
    private BigDecimal waterAmount;
    private BigDecimal irrigationHours;

    public IrrigationCalendarEventDTO(Irrigation irrigation) {
        this.irrigationId = irrigation.getId();
        this.equipmentName = irrigation.getEquipment() != null ? irrigation.getEquipment().getName() : "N/A";
        this.waterAmount = irrigation.getWaterAmount();
        this.irrigationHours = irrigation.getIrrigationHours();
    }
}