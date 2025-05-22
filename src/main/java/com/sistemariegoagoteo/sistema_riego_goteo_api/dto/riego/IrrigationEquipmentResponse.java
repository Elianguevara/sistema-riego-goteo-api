package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class IrrigationEquipmentResponse {
    private Integer id;
    private String name;
    private BigDecimal measuredFlow;
    private Boolean hasFlowMeter;
    private String equipmentType;
    private String equipmentStatus;
    private Integer farmId;
    private String farmName;

    public IrrigationEquipmentResponse(IrrigationEquipment equipment) {
        this.id = equipment.getId();
        this.name = equipment.getName();
        this.measuredFlow = equipment.getMeasuredFlow();
        this.hasFlowMeter = equipment.getHasFlowMeter();
        this.equipmentType = equipment.getEquipmentType();
        this.equipmentStatus = equipment.getEquipmentStatus();
        if (equipment.getFarm() != null) {
            this.farmId = equipment.getFarm().getId();
            this.farmName = equipment.getFarm().getName();
        }
    }
}