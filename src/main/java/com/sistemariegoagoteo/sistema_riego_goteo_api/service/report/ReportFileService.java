package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.OperationsLogReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.PeriodSummaryReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.WaterBalanceReportDTO;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.stream.Stream;

@Service
public class ReportFileService {

    // =====================================================================================
    // == WATER BALANCE REPORT
    // =====================================================================================

    public void generateWaterBalancePdf(WaterBalanceReportDTO data, java.io.OutputStream outputStream)
            throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        document.add(new Paragraph("Reporte de Balance Hídrico", titleFont));
        if (data.getFarmName() != null)
            document.add(new Paragraph("Finca: " + data.getFarmName()));
        if (data.getDateRange() != null)
            document.add(
                    new Paragraph("Período: " + data.getDateRange().getStart() + " a " + data.getDateRange().getEnd()));
        document.add(Chunk.NEWLINE);

        if (data.getSectors() != null && !data.getSectors().isEmpty()) {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            addTableHeader(table, "Sector", "Agua Riego Total (m³)", "Lluvia Efectiva Total (mm)", "Horas Riego Total");
            for (WaterBalanceReportDTO.SectorData sector : data.getSectors()) {
                table.addCell(sector.getSectorName());
                table.addCell(String.valueOf(sector.getSummary().getTotalIrrigationWater()));
                table.addCell(String.valueOf(sector.getSummary().getTotalEffectiveRain()));
                table.addCell(String.valueOf(sector.getSummary().getTotalIrrigationHours()));
            }
            document.add(table);
        } else {
            document.add(new Paragraph("No se encontraron datos para los sectores en el período seleccionado."));
        }

        document.close();
    }

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

    public void generateOperationsLogPdf(OperationsLogReportDTO data, java.io.OutputStream outputStream)
            throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        document.add(new Paragraph("Reporte de Bitácora de Operaciones", titleFont));
        if (data.getFarmName() != null)
            document.add(new Paragraph("Finca: " + data.getFarmName()));
        if (data.getDateRange() != null)
            document.add(
                    new Paragraph("Período: " + data.getDateRange().getStart() + " a " + data.getDateRange().getEnd()));
        document.add(Chunk.NEWLINE);

        if (data.getOperations() != null && !data.getOperations().isEmpty()) {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            addTableHeader(table, "Fecha y Hora", "Tipo", "Descripción", "Ubicación", "Usuario");
            for (OperationsLogReportDTO.Operation op : data.getOperations()) {
                table.addCell(op.getDatetime());
                table.addCell(op.getType());
                table.addCell(op.getDescription());
                table.addCell(op.getLocation());
                table.addCell(op.getUserName());
            }
            document.add(table);
        } else {
            document.add(new Paragraph("No se encontraron operaciones para el período seleccionado."));
        }

        document.close();
    }

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

    public void generatePeriodSummaryPdf(PeriodSummaryReportDTO data, java.io.OutputStream outputStream)
            throws DocumentException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);

        document.add(new Paragraph("Reporte de Resumen del Período", titleFont));
        if (data.getFarmName() != null)
            document.add(new Paragraph("Finca: " + data.getFarmName()));
        if (data.getPeriod() != null)
            document.add(new Paragraph("Período: " + data.getPeriod().getStart() + " a " + data.getPeriod().getEnd()));
        document.add(Chunk.NEWLINE);

        // Water Summary Section
        document.add(new Paragraph("Resumen Hídrico", sectionFont));
        if (data.getWaterSummary() != null) {
            document.add(new Paragraph(
                    "Agua de Riego Total: " + data.getWaterSummary().getTotalIrrigationWaterM3() + " m³"));
            document.add(new Paragraph("Horas de Riego Total: " + data.getWaterSummary().getTotalIrrigationHours()));
            document.add(new Paragraph(
                    "Precipitación Total: " + data.getWaterSummary().getTotalPrecipitationMM() + " mm"));
        }
        document.add(Chunk.NEWLINE);

        // Operations Summary Section
        document.add(new Paragraph("Resumen de Operaciones", sectionFont));
        if (data.getOperationsSummary() != null) {
            document.add(new Paragraph("Tareas Creadas: " + data.getOperationsSummary().getTasksCreated()));
            document.add(new Paragraph("Tareas Completadas: " + data.getOperationsSummary().getTasksCompleted()));
            document.add(new Paragraph(
                    "Registros de Mantenimiento: " + data.getOperationsSummary().getMaintenanceRecords()));
        }
        document.add(Chunk.NEWLINE);

        document.close();
    }

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

    // =====================================================================================
    // == UTILITY METHODS
    // =====================================================================================
    private void addTableHeader(PdfPTable table, String... headers) {
        Stream.of(headers).forEach(columnTitle -> {
            PdfPCell header = new PdfPCell();
            header.setBackgroundColor(Color.LIGHT_GRAY);
            header.setBorderWidth(2);
            header.setPhrase(new Phrase(columnTitle));
            table.addCell(header);
        });
    }
}
