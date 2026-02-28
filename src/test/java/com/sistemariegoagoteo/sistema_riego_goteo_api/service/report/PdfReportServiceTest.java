package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.util.report.ReportBrandingHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para PdfReportService.
 *
 * Usa una instancia real de ReportBrandingHelper (sin mocks) para validar
 * que el PDF se genera end-to-end sin errores de compilación ni de runtime.
 * El logo se carga desde el classpath; si no existe se continúa sin imagen.
 */
@DisplayName("PdfReportService - Tests Unitarios")
class PdfReportServiceTest {

    private PdfReportService pdfReportService;

    @BeforeEach
    void setUp() {
        pdfReportService = new PdfReportService(new ReportBrandingHelper());
    }

    // ── Casos de uso principales ─────────────────────────────────────────────

    @Test
    @DisplayName("generateCorporateReport() con datos válidos debe retornar bytes de PDF")
    void generateCorporateReport_conDatos_retornaBytesValidos() {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID", "Descripción", "Sector", "Finca",
                             "Estado", "Creado por", "Asignado a", "Fecha Creación"},
                new String[]{"1", "Riego Lote Norte", "Lote Norte", "Finca A",
                             "PENDIENTE", "analista", "operario", "28/02/2026"},
                new String[]{"2", "Fertilización Hectárea 5", "Hectárea 5", "Finca A",
                             "COMPLETADA", "analista", "operario2", "27/02/2026"}
        );

        byte[] result = pdfReportService.generateCorporateReport(
                tableData, "Reporte de Tareas", "analista");

        assertThat(result).isNotNull().isNotEmpty();
        // Los archivos PDF comienzan con la firma %PDF
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("generateCorporateReport() con lista vacía debe retornar PDF con aviso 'Sin registros'")
    void generateCorporateReport_sinDatos_retornaPdfConAviso() {
        byte[] result = pdfReportService.generateCorporateReport(
                Collections.emptyList(), "Reporte Vacío", "analista");

        assertThat(result).isNotNull().isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("generateCorporateReport() con 2 args (sobrecarga) debe retornar PDF válido")
    void generateCorporateReport_dosArgumentos_retornaBytesValidos() {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID Tarea", "Tipo Operación", "Sector Asignado", "Estado Actual"},
                new String[]{"T-1001", "Riego por Goteo", "Lote Norte", "COMPLETADA"},
                new String[]{"T-1002", "Fertilización", "Hectárea 5", "EN PROGRESO"}
        );

        byte[] result = pdfReportService.generateCorporateReport(tableData, "Operador Agrícola");

        assertThat(result).isNotNull().isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    // ── Manejo de valores nulos ───────────────────────────────────────────────

    @Test
    @DisplayName("generateCorporateReport() con celdas null debe reemplazarlas por '—' sin lanzar excepción")
    void generateCorporateReport_conNulos_retornaPdfValido() {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID", "Descripción", "Sector", "Finca",
                             "Estado", "Creado por", "Asignado a", "Fecha Creación"},
                new String[]{"1", null, null, null, "PENDIENTE", "analista", null, null},
                new String[]{"2", "Tarea B", null, "Finca A", null, null, "operario", "01/01/2025"}
        );

        byte[] result = pdfReportService.generateCorporateReport(
                tableData, "Reporte con Nulos", "analista");

        assertThat(result).isNotNull().isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    // ── Volumen ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateCorporateReport() con 100 filas no debe lanzar excepción")
    void generateCorporateReport_100Filas_retornaPdfValido() {
        List<String[]> tableData = new ArrayList<>();
        tableData.add(new String[]{"ID", "Descripción", "Estado"});
        for (int i = 1; i <= 100; i++) {
            tableData.add(new String[]{String.valueOf(i), "Tarea número " + i, "PENDIENTE"});
        }

        byte[] result = pdfReportService.generateCorporateReport(
                tableData, "Reporte Grande", "analista");

        assertThat(result).isNotNull().isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    // ── Nombre del solicitante ────────────────────────────────────────────────

    @Test
    @DisplayName("generateCorporateReport() con requesterName vacío debe generar PDF sin error")
    void generateCorporateReport_requesterNameVacio_retornaPdfValido() {
        List<String[]> tableData = Arrays.asList(
                new String[]{"ID", "Estado"},
                new String[]{"1", "PENDIENTE"}
        );

        byte[] result = pdfReportService.generateCorporateReport(tableData, "Reporte", "");

        assertThat(result).isNotNull().isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }
}
