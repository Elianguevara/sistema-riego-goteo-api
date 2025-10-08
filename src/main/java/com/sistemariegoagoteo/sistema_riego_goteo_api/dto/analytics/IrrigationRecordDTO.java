package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class IrrigationRecordDTO {
    private Integer id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date startDateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date endDateTime;
    private BigDecimal waterAmount;
    private BigDecimal irrigationHours;
    private Integer sectorId;
    private String sectorName;
    private Integer equipmentId;
    private String equipmentName;

    public IrrigationRecordDTO(Irrigation irrigation) {
        this.id = irrigation.getId();
        this.startDateTime = irrigation.getStartDatetime();
        this.endDateTime = irrigation.getEndDatetime();
        this.waterAmount = irrigation.getWaterAmount();
        this.irrigationHours = irrigation.getIrrigationHours();
        if (irrigation.getSector() != null) {
            this.sectorId = irrigation.getSector().getId();
            this.sectorName = irrigation.getSector().getName();
        }
        if (irrigation.getEquipment() != null) {
            this.equipmentId = irrigation.getEquipment().getId();
            this.equipmentName = irrigation.getEquipment().getName();
        }
    }
}