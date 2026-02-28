package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.util.report.ReportBrandingHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Servicio responsable de generar documentos PDF corporativos con:
 *   - Membrete profesional (logo, empresa, título, fecha) vía {@link ReportBrandingHelper}.
 *   - Pie de página con numeración "Página X de Y" usando PdfTemplate two-pass.
 *   - Tabla de datos con encabezados en verde #10b981 y zebra striping.
 *   - Mensaje de aviso cuando no hay registros disponibles.
 *
 * La gestión de marca (colores, logo, footer) está completamente delegada a
 * {@link ReportBrandingHelper}, de modo que cualquier cambio de identidad
 * corporativa se aplica desde un único punto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final ReportBrandingHelper brandingHelper;

    /**
     * Título por defecto usado cuando el llamador no especifica uno.
     * Mantiene compatibilidad con el endpoint {@code /download/pdf} existente.
     */
    private static final String DEFAULT_TITLE = "Reporte de Operaciones Agrícolas";

    // ════════════════════════════════════════════════════════════════════════
    //  API PÚBLICA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Sobrecarga de compatibilidad para el endpoint {@code GET /api/reports/download/pdf}.
     * Delega en {@link #generateCorporateReport(List, String, String)} con el título
     * por defecto.
     *
     * @param tableData     Filas de datos: la primera fila [0] son los encabezados de columna
     * @param requesterName Nombre del usuario auditor que solicita el reporte
     * @return Bytes del PDF generado, listos para enviar como respuesta HTTP
     */
    public byte[] generateCorporateReport(List<String[]> tableData, String requesterName) {
        return generateCorporateReport(tableData, DEFAULT_TITLE, requesterName);
    }

    /**
     * Genera un documento PDF corporativo completo.
     *
     * Flujo de construcción:
     * <ol>
     *   <li>Crea el escritor y registra el footer <strong>antes</strong> de {@code document.open()}</li>
     *   <li>Inserta el membrete corporativo vía {@link ReportBrandingHelper#buildPdfHeader}</li>
     *   <li>Construye la tabla de datos (o muestra aviso si no hay registros)</li>
     * </ol>
     *
     * Márgenes del documento (en puntos):
     * <pre>
     *   Izquierdo = 36  |  Derecho = 36  |  Superior = 60  |  Inferior = 50
     * </pre>
     * El margen inferior de 50 pt garantiza que el footer (posicionado a 18 pt
     * desde el borde absoluto de la página) nunca solape el contenido.
     *
     * @param tableData     Filas de datos: fila [0] contiene los títulos de columna
     * @param reportTitle   Título visible en el membrete del reporte
     * @param requesterName Nombre del usuario que solicita el documento
     * @return Bytes del PDF listo para descarga
     * @throws RuntimeException si OpenPDF no puede construir el documento
     */
    public byte[] generateCorporateReport(List<String[]> tableData,
                                          String reportTitle,
                                          String requesterName) {

        // Márgenes: left, right, top, bottom  (unidad: puntos tipográficos)
        // Bottom = 50 pt deja espacio suficiente al footer dibujado en 18 pt desde el borde.
        Document document = new Document(PageSize.A4, 36f, 36f, 60f, 50f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // ── PASO CLAVE: registrar el footer ANTES de document.open() ────────
            // PdfPageEventHelper solo intercepta eventos si está registrado antes
            // de que el escritor empiece a procesar páginas.
            writer.setPageEvent(new ReportBrandingHelper.PdfPageNumberFooter());

            document.open();

            // 1. Membrete: logo (izquierda) + empresa / título / fecha (derecha)
            brandingHelper.buildPdfHeader(document, reportTitle, requesterName);

            // 2. Tabla de datos (o aviso si la lista está vacía)
            buildDataTable(document, tableData);

            document.close();

        } catch (DocumentException e) {
            log.error("[PdfReportService] Error al construir el PDF '{}': {}", reportTitle, e.getMessage(), e);
            throw new RuntimeException("No se pudo generar el reporte PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE LA TABLA DE DATOS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Agrega la tabla de datos al documento o, si no hay registros,
     * un párrafo de aviso centrado.
     *
     * Convención de {@code tableData}:
     * <ul>
     *   <li>Índice [0]   → array de nombres de columna (encabezados)</li>
     *   <li>Índices [1…] → arrays de valores por fila</li>
     * </ul>
     */
    private void buildDataTable(Document document, List<String[]> tableData)
            throws DocumentException {

        // ── Caso sin datos ───────────────────────────────────────────────────
        if (tableData == null || tableData.isEmpty()) {
            Font warningFont = FontFactory.getFont(
                    FontFactory.HELVETICA_OBLIQUE, 11f, ReportBrandingHelper.PDF_SUBTLE_GRAY);
            Paragraph noData = new Paragraph("Sin registros disponibles.", warningFont);
            noData.setAlignment(Element.ALIGN_CENTER);
            noData.setSpacingBefore(24f);
            document.add(noData);
            return;
        }

        // ── Crear tabla con tantas columnas como campos tenga la primera fila ─
        int numCols = tableData.get(0).length;
        PdfPTable table = new PdfPTable(numCols);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(12f);

        // ── Fila de encabezados ──────────────────────────────────────────────
        addTableHeaders(table, tableData.get(0));

        // ── Filas de datos con zebra striping ───────────────────────────────
        addTableRows(table, tableData);

        document.add(table);
    }

    /**
     * Dibuja la fila de encabezados con fondo verde #10b981 y texto blanco en negrita.
     *
     * @param table   Tabla OpenPDF ya inicializada
     * @param headers Array de títulos de columna (fila [0] de tableData)
     */
    private void addTableHeaders(PdfPTable table, String[] headers) {
        Font headerFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 11f, ReportBrandingHelper.PDF_WHITE);

        for (String headerText : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(headerText, headerFont));

            // Fondo verde esmeralda corporativo (#10b981)
            cell.setBackgroundColor(ReportBrandingHelper.PDF_GREEN);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(9f);
            cell.setBorderColor(ReportBrandingHelper.PDF_BORDER);

            table.addCell(cell);
        }
    }

    /**
     * Añade las filas de datos (índice 1 en adelante) aplicando zebra striping:
     * <ul>
     *   <li>Filas pares   → {@link ReportBrandingHelper#PDF_EVEN_ROW} (verde muy pálido)</li>
     *   <li>Filas impares → blanco</li>
     * </ul>
     * Las celdas con valor {@code null} se muestran como "—" (guión largo).
     */
    private void addTableRows(PdfPTable table, List<String[]> tableData) {
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, Color.BLACK);

        // i = 1 → omitimos la fila [0] que ya fue usada como encabezado
        for (int i = 1; i < tableData.size(); i++) {
            // Alternancia de fondo: par = verde pálido / impar = blanco
            Color rowBg = (i % 2 == 0)
                    ? ReportBrandingHelper.PDF_EVEN_ROW
                    : Color.WHITE;

            for (String cellValue : tableData.get(i)) {
                String displayValue = (cellValue != null && !cellValue.isBlank())
                        ? cellValue
                        : "\u2014"; // "—" como valor nulo legible

                PdfPCell cell = new PdfPCell(new Phrase(displayValue, dataFont));
                cell.setBackgroundColor(rowBg);
                cell.setPadding(6f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBorderColor(ReportBrandingHelper.PDF_BORDER);

                table.addCell(cell);
            }
        }
    }
}
