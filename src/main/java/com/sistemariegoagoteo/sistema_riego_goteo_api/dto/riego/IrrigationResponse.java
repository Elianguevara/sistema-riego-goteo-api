package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class IrrigationResponse {
    private Integer id;

    private Integer sectorId;
    private String sectorName;

    private Integer equipmentId;
    private String equipmentName;

    private Integer farmId; // Derivado del sector
    private String farmName; // Derivado del sector

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDateTime;

    private BigDecimal waterAmount;
    private BigDecimal irrigationHours;
    /** true = el operario sobrescribió el valor con la lectura del caudalímetro. */
    private Boolean isManualWaterVolume;
    private String message;

    public IrrigationResponse(Irrigation irrigation) {
        this.id = irrigation.getId();
        if (irrigation.getSector() != null) {
            this.sectorId = irrigation.getSector().getId();
            this.sectorName = irrigation.getSector().getName();
            if (irrigation.getSector().getFarm() != null) {
                this.farmId = irrigation.getSector().getFarm().getId();
                this.farmName = irrigation.getSector().getFarm().getName();
            }
        }
        if (irrigation.getEquipment() != null) {
            this.equipmentId = irrigation.getEquipment().getId();
            this.equipmentName = irrigation.getEquipment().getName();
        }
        this.startDateTime = irrigation.getStartDatetime();
        this.endDateTime = irrigation.getEndDatetime();
        this.waterAmount = irrigation.getWaterAmount();
        this.irrigationHours = irrigation.getIrrigationHours();
        this.isManualWaterVolume = irrigation.getIsManualWaterVolume();
        this.message = "Riego registrado exitosamente. ID: " + irrigation.getId();
    }
}