package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.opencsv.CSVWriter;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.OperationsLogReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.PeriodSummaryReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.WaterBalanceReportDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ReportFileService {

    // =====================================================================================
    // == WATER BALANCE REPORT
    // =====================================================================================

    public void generateWaterBalanceCsv(WaterBalanceReportDTO data, java.io.Writer writer) throws IOException {
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(
                    new String[] { "sectorId", "sectorName", "totalIrrigationWater_m3", "totalEffectiveRain_mm",
                            "totalIrrigationHours" });
            if (data.getSectors() != null) {
                for (WaterBalanceReportDTO.SectorData sector : data.getSectors()) {
                    csvWriter.writeNext(new String[] {
                            sector.getSectorId().toString(),
                            sector.getSectorName(),
                            sector.getSummary().getTotalIrrigationWater().toString(),
                            sector.getSummary().getTotalEffectiveRain().toString(),
                            sector.getSummary().getTotalIrrigationHours().toString()
                    });
                }
            }
        }
    }

    // =====================================================================================
    // == OPERATIONS LOG REPORT
    // =====================================================================================

    public void generateOperationsLogCsv(OperationsLogReportDTO data, java.io.Writer writer) throws IOException {
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(new String[] { "datetime", "type", "description", "location", "userName" });
            if (data.getOperations() != null) {
                for (OperationsLogReportDTO.Operation op : data.getOperations()) {
                    csvWriter.writeNext(new String[] {
                            op.getDatetime(),
                            op.getType(),
                            op.getDescription(),
                            op.getLocation(),
                            op.getUserName()
                    });
                }
            }
        }
    }

    // =====================================================================================
    // == PERIOD SUMMARY REPORT
    // =====================================================================================

    public void generatePeriodSummaryCsv(PeriodSummaryReportDTO data, java.io.Writer writer) throws IOException {
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(new String[] { "Métrica", "Valor" });
            if (data.getWaterSummary() != null) {
                csvWriter.writeNext(new String[] { "Agua de Riego Total (m³)",
                        data.getWaterSummary().getTotalIrrigationWaterM3().toString() });
                csvWriter.writeNext(new String[] { "Horas de Riego Total",
                        data.getWaterSummary().getTotalIrrigationHours().toString() });
            }
            if (data.getOperationsSummary() != null) {
                csvWriter.writeNext(new String[] { "Tareas Creadas",
                        String.valueOf(data.getOperationsSummary().getTasksCreated()) });
                csvWriter.writeNext(new String[] { "Tareas Completadas",
                        String.valueOf(data.getOperationsSummary().getTasksCompleted()) });
            }
        }
    }
}
