package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaterBalanceDTO {
    private Date date;
    private BigDecimal irrigationWater; // Agua por riego
    private BigDecimal effectiveRain;   // Agua por lluvia efectiva
    // Podrías añadir aquí la Evapotranspiración (ETc) en el futuro
}