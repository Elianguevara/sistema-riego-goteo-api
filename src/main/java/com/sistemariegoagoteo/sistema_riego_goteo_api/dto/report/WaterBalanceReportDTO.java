package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class WaterBalanceReportDTO {
    private String farmName;
    private DateRange dateRange;
    private List<SectorData> sectors;
    private FarmTotals farmTotals;

    @Data
    public static class DateRange {
        private String start;
        private String end;
    }

    @Data
    public static class SectorData {
        private Integer sectorId;
        private String sectorName;
        private Summary summary;
        private List<DailyData> dailyData;
    }

    @Data
    public static class Summary {
        private BigDecimal totalIrrigationWater;
        private BigDecimal totalEffectiveRain;
        private BigDecimal totalIrrigationHours;
    }

    @Data
    public static class DailyData {
        private String date;
        private BigDecimal irrigationWater;
        private BigDecimal effectiveRain;
    }

    @Data
    public static class FarmTotals {
        private BigDecimal totalIrrigationWater;
        private BigDecimal totalEffectiveRain;
        private BigDecimal totalIrrigationHours;
    }
}