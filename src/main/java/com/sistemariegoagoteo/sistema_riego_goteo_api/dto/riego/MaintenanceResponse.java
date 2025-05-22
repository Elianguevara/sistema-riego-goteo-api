package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Maintenance;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class MaintenanceResponse {
    private Integer id;

    private Integer equipmentId;
    private String equipmentName;

    private Integer farmId; // Derivado del equipo
    private String farmName; // Derivado del equipo

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") // Solo fecha
    private Date date;
    private String description;
    private BigDecimal workHours;

    public MaintenanceResponse(Maintenance maintenance) {
        this.id = maintenance.getId();
        if (maintenance.getIrrigationEquipment() != null) {
            this.equipmentId = maintenance.getIrrigationEquipment().getId();
            this.equipmentName = maintenance.getIrrigationEquipment().getName();
            if (maintenance.getIrrigationEquipment().getFarm() != null) {
                this.farmId = maintenance.getIrrigationEquipment().getFarm().getId();
                this.farmName = maintenance.getIrrigationEquipment().getFarm().getName();
            }
        }
        this.date = maintenance.getDate();
        this.description = maintenance.getDescription();
        this.workHours = maintenance.getWorkHours();
    }
}