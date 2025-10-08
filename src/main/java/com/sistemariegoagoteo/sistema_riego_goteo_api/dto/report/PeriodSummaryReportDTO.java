package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PeriodSummaryReportDTO {
    private String farmName;
    private Period period;
    private WaterSummary waterSummary;
    private OperationsSummary operationsSummary;
    private List<WaterUsageBySector> waterUsageBySector;

    @Data
    public static class Period {
        private String start;
        private String end;
    }

    @Data
    public static class WaterSummary {
        private BigDecimal totalIrrigationWaterM3;
        private BigDecimal totalIrrigationHours;
        private BigDecimal totalPrecipitationMM;
        private BigDecimal totalEffectivePrecipitationMM;
    }

    @Data
    public static class OperationsSummary {
        private long tasksCreated;
        private long tasksCompleted;
        private long maintenanceRecords;
        private long fertilizationRecords;
    }

    @Data
    public static class WaterUsageBySector {
        private String sectorName;
        private BigDecimal totalWaterM3;
    }
}