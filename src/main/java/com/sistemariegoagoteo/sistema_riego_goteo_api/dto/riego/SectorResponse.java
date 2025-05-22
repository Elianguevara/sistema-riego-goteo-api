package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SectorResponse {
    private Integer id;
    private String name;
    private Integer farmId;
    private String farmName; // Útil para mostrar en el frontend
    private Integer equipmentId;
    private String equipmentName; // Útil para mostrar

    public SectorResponse(Sector sector) {
        this.id = sector.getId();
        this.name = sector.getName();
        if (sector.getFarm() != null) {
            this.farmId = sector.getFarm().getId();
            this.farmName = sector.getFarm().getName();
        }
        if (sector.getEquipment() != null) {
            this.equipmentId = sector.getEquipment().getId();
            this.equipmentName = sector.getEquipment().getName();
        }
    }
}