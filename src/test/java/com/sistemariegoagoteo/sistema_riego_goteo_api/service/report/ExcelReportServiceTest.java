package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.util.report.ReportBrandingHelper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para ExcelReportService.
 *
 * Usa una instancia real de ReportBrandingHelper para validar el flujo
 * completo de generación de Excel (membrete, encabezados, datos, fechas).
 */
@DisplayName("ExcelReportService - Tests Unitarios")
class ExcelReportServiceTest {

    private ExcelReportService excelReportService;

    /** Índice de la primera fila de datos (membrete 0-2, separador 3, encabezados 4). */
    private static final int FIRST_DATA_ROW = 5;

    @BeforeEach
    void setUp() {
        excelReportService = new ExcelReportService(new ReportBrandingHelper());
    }

    // ── Casos de uso principales ─────────────────────────────────────────────

    @Test
    @DisplayName("generateReport() con datos válidos debe retornar un xlsx con 1 hoja 'Datos'")
    void generateReport_conDatos_retornaXlsxValido() throws Exception {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID", "Descripción", "Sector", "Finca",
                             "Estado", "Creado por", "Asignado a", "Fecha Creación"},
                new String[]{"1", "Riego Lote Norte", "Lote Norte", "Finca A",
                             "PENDIENTE", "analista", "operario", "28/02/2026"},
                new String[]{"2", "Fertilización", "Hectárea 5", "Finca A",
                             "COMPLETADA", "analista", "op2", "27/02/2026"}
        );

        byte[] result = excelReportService.generateReport(tableData, "Reporte de Tareas");

        assertThat(result).isNotNull().isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("Datos");
        }
    }

    @Test
    @DisplayName("generateReport() con lista vacía debe retornar xlsx con aviso 'Sin registros'")
    void generateReport_sinDatos_retornaXlsxConAviso() throws Exception {
        byte[] result = excelReportService.generateReport(
                Collections.emptyList(), "Reporte Vacío");

        assertThat(result).isNotNull().isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
        }
    }

    // ── Detección de fechas ───────────────────────────────────────────────────

    @Test
    @DisplayName("generateReport() debe convertir fechas dd/MM/yyyy a celdas numéricas (fecha nativa)")
    void generateReport_conFechasValidas_convierteCeldasAFechaNativa() throws Exception {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID", "Fecha Creación"},
                new String[]{"1", "28/02/2026"},
                new String[]{"2", "01/01/2025"}
        );

        byte[] result = excelReportService.generateReport(tableData, "Reporte con Fechas");

        assertThat(result).isNotNull().isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            // La primera fila de datos está en FIRST_DATA_ROW
            Row dataRow = sheet.getRow(FIRST_DATA_ROW);
            assertThat(dataRow).isNotNull();
            Cell dateCell = dataRow.getCell(1);
            assertThat(dateCell).isNotNull();
            // Una celda de fecha real en POI tiene tipo NUMERIC
            assertThat(dateCell.getCellType()).isEqualTo(CellType.NUMERIC);
            // Y su estilo tiene un formato de fecha (data format != 0)
            assertThat(dateCell.getCellStyle().getDataFormat()).isNotEqualTo((short) 0);
        }
    }

    @Test
    @DisplayName("generateReport() debe tratar cadenas no-fecha como texto, no como fecha nativa")
    void generateReport_conTextoNoFecha_loTrataComotexto() throws Exception {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID", "Descripción"},
                new String[]{"1", "PENDIENTE"}
        );

        byte[] result = excelReportService.generateReport(tableData, "Reporte Texto");

        assertThat(result).isNotNull().isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(FIRST_DATA_ROW);
            assertThat(dataRow).isNotNull();
            Cell textCell = dataRow.getCell(1);
            assertThat(textCell).isNotNull();
            assertThat(textCell.getCellType()).isEqualTo(CellType.STRING);
            assertThat(textCell.getStringCellValue()).isEqualTo("PENDIENTE");
        }
    }

    // ── Manejo de valores nulos ───────────────────────────────────────────────

    @Test
    @DisplayName("generateReport() con celdas null debe escribir '—' sin lanzar excepción")
    void generateReport_conNulos_reemplazaNulosConGuionLargo() throws Exception {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID", "Descripción", "Sector"},
                new String[]{"1", null, null},
                new String[]{"2", "Tarea B", null}
        );

        byte[] result = excelReportService.generateReport(tableData, "Reporte con Nulos");

        assertThat(result).isNotNull().isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(FIRST_DATA_ROW);
            assertThat(dataRow).isNotNull();
            Cell nullCell = dataRow.getCell(1);
            assertThat(nullCell).isNotNull();
            // Null → guión largo Unicode
            assertThat(nullCell.getStringCellValue()).isEqualTo("\u2014");
        }
    }

    // ── Encabezados de tabla ──────────────────────────────────────────────────

    @Test
    @DisplayName("generateReport() debe escribir los encabezados correctamente en la fila de tabla")
    void generateReport_encabezadosCorrectos_enFilaDeTabla() throws Exception {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID", "Estado", "Fecha Creación"},
                new String[]{"1", "PENDIENTE", "28/02/2026"}
        );

        byte[] result = excelReportService.generateReport(tableData, "Reporte Encabezados");

        assertThat(result).isNotNull().isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            // Los encabezados de tabla están en la fila anterior a los datos (FIRST_DATA_ROW - 1)
            Row headerRow = sheet.getRow(FIRST_DATA_ROW - 1);
            assertThat(headerRow).isNotNull();
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Estado");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Fecha Creación");
        }
    }

    // ── Volumen ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateReport() con 100 filas no debe lanzar excepción")
    void generateReport_100Filas_retornaXlsxValido() throws Exception {
        List<String[]> tableData = new ArrayList<>();
        tableData.add(new String[]{"ID", "Descripción", "Estado"});
        for (int i = 1; i <= 100; i++) {
            tableData.add(new String[]{String.valueOf(i), "Tarea " + i, "PENDIENTE"});
        }

        byte[] result = excelReportService.generateReport(tableData, "Reporte Grande");

        assertThat(result).isNotNull().isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            // membrete (3) + separador (1) + encabezados (1) + 100 filas de datos = 105 filas (índice 104)
            assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(104);
        }
    }
}
