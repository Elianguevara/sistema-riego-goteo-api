package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.util.report.ReportBrandingHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio responsable de generar reportes Excel (.xlsx) corporativos con:
 *   - Membrete profesional (logo, empresa, título, fecha) vía {@link ReportBrandingHelper}.
 *   - Tabla de datos con encabezados en verde #10b981, negrita y bordes finos.
 *   - Zebra striping (alternancia de colores) en las filas de datos.
 *   - Detección automática de fechas: si el valor coincide con dd/MM/yyyy,
 *     se escribe como celda de fecha real (no string) con formato visual adecuado.
 *   - AutoSizeColumn con padding del 10% para todas las columnas.
 *   - Mensaje de aviso si no hay registros disponibles.
 *
 * Convención de {@code tableData}:
 * <ul>
 *   <li>Índice [0]   → array de nombres de columna (encabezados)</li>
 *   <li>Índices [1…] → arrays de valores de cada fila</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelReportService {

    private final ReportBrandingHelper brandingHelper;

    /** Nombre de la hoja dentro del libro Excel. */
    private static final String SHEET_NAME = "Datos";

    /** Patrón para detectar fechas con formato dd/MM/yyyy (opcionalmente con hora). */
    private static final Pattern DATE_PATTERN =
            Pattern.compile("^\\d{2}/\\d{2}/\\d{4}.*");

    /** Formateador para parsear fechas detectadas (solo parte de la fecha). */
    private static final DateTimeFormatter DATE_PARSE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ════════════════════════════════════════════════════════════════════════
    //  API PÚBLICA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Genera un reporte Excel corporativo y lo serializa a bytes.
     *
     * @param tableData   Filas de datos; fila [0] contiene los títulos de columna
     * @param reportTitle Título visible en el membrete del reporte
     * @return Bytes del archivo .xlsx, listos para enviar como respuesta HTTP
     * @throws RuntimeException si Apache POI no puede construir el libro
     */
    public byte[] generateReport(List<String[]> tableData, String reportTitle) {

        // XSSFWorkbook implementa Closeable: try-with-resources lo libera al finalizar
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet sheet = workbook.createSheet(SHEET_NAME);

            // 1. Membrete corporativo: retorna el índice de la primera fila libre
            int firstDataRow = brandingHelper.buildExcelHeader(workbook, sheet, reportTitle);

            // 2. Tabla de datos (encabezados + filas) a partir de esa fila
            buildDataTable(workbook, sheet, tableData, firstDataRow);

            // 3. Serializar el libro a bytes en memoria
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("[ExcelReportService] Error al generar Excel '{}': {}",
                    reportTitle, e.getMessage(), e);
            throw new RuntimeException("No se pudo generar el reporte Excel: " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE LA TABLA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Construye la tabla de datos en la hoja a partir de {@code startRow}.
     * Si {@code tableData} es nulo o vacío, escribe un aviso en su lugar.
     */
    private void buildDataTable(XSSFWorkbook workbook, XSSFSheet sheet,
                                List<String[]> tableData, int startRow) {

        // ── Caso sin datos ───────────────────────────────────────────────────
        if (tableData == null || tableData.isEmpty()) {
            CellStyle warnStyle = createWarningStyle(workbook);
            Row row = sheet.createRow(startRow + 1);
            row.setHeightInPoints(20f);
            Cell cell = row.createCell(0);
            cell.setCellValue("Sin registros disponibles.");
            cell.setCellStyle(warnStyle);
            return;
        }

        // ── Pre-construir estilos (se crean una vez y se reutilizan en todas las celdas) ─
        // Nota: POI tiene un límite de ~64.000 estilos por libro; crearlos fuera del
        // bucle es mandatorio para evitar desbordamiento en reportes con muchas filas.
        XSSFCellStyle headerStyle   = createHeaderStyle(workbook);
        XSSFCellStyle evenDataStyle = createDataStyle(workbook, /* isEven */ true,  /* isDate */ false);
        XSSFCellStyle oddDataStyle  = createDataStyle(workbook, /* isEven */ false, /* isDate */ false);
        XSSFCellStyle evenDateStyle = createDataStyle(workbook, /* isEven */ true,  /* isDate */ true);
        XSSFCellStyle oddDateStyle  = createDataStyle(workbook, /* isEven */ false, /* isDate */ true);

        String[] headers = tableData.get(0);

        // ── Fila de encabezados ──────────────────────────────────────────────
        addHeaderRow(sheet, startRow, headers, headerStyle);

        // ── Filas de datos ───────────────────────────────────────────────────
        for (int i = 1; i < tableData.size(); i++) {
            boolean isEven = (i % 2 == 0);
            addDataRow(sheet, startRow + i, tableData.get(i),
                    isEven ? evenDataStyle : oddDataStyle,
                    isEven ? evenDateStyle : oddDateStyle);
        }

        // ── AutoSizeColumn con +10% de margen para todos los encabezados ─────
        // Se hace al final para que POI mida el contenido definitivo de cada columna.
        // Cap máximo de 15.000 unidades (~116 caracteres) para evitar columnas excesivas.
        for (int col = 0; col < headers.length; col++) {
            sheet.autoSizeColumn(col);
            int adjusted = (int) (sheet.getColumnWidth(col) * 1.10);
            sheet.setColumnWidth(col, Math.min(adjusted, 15_000));
        }
    }

    /**
     * Escribe la fila de encabezados con fondo verde corporativo (#10b981),
     * fuente blanca en negrita, texto centrado y bordes finos en los 4 lados.
     */
    private void addHeaderRow(XSSFSheet sheet, int rowIdx,
                              String[] headers, XSSFCellStyle style) {
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(22f);

        for (int col = 0; col < headers.length; col++) {
            Cell cell = row.createCell(col);
            cell.setCellValue(headers[col]);
            cell.setCellStyle(style);
        }
    }

    /**
     * Escribe una fila de datos aplicando detección de fechas por celda.
     *
     * Si el valor de una celda coincide con el patrón {@code dd/MM/yyyy},
     * se parsea a {@link LocalDate} y se escribe como celda de fecha real
     * (no como string), usando el estilo con formato de fecha correspondiente.
     * Esto permite que el usuario de Excel pueda ordenar y filtrar por fecha.
     *
     * @param dataStyle estilo de fondo (par/impar) para celdas de texto
     * @param dateStyle estilo de fondo + formato fecha (par/impar)
     */
    private void addDataRow(XSSFSheet sheet, int rowIdx, String[] rowValues,
                            XSSFCellStyle dataStyle, XSSFCellStyle dateStyle) {
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(18f);

        for (int col = 0; col < rowValues.length; col++) {
            Cell cell = row.createCell(col);
            String value = rowValues[col];

            if (value == null || value.isBlank()) {
                // Valor nulo → guión largo como convención tipográfica
                cell.setCellValue("\u2014");
                cell.setCellStyle(dataStyle);
                continue;
            }

            // Detección de fecha: si coincide con dd/MM/yyyy, convertir a fecha real
            if (DATE_PATTERN.matcher(value).matches()) {
                try {
                    // Extraer solo los primeros 10 caracteres (dd/MM/yyyy)
                    LocalDate date = LocalDate.parse(value.substring(0, 10), DATE_PARSE_FMT);
                    // Convertir a java.util.Date que POI entiende como fecha Excel
                    Date utilDate = java.util.Date.from(
                            date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                    cell.setCellValue(utilDate);
                    cell.setCellStyle(dateStyle);
                    continue;
                } catch (DateTimeParseException ignored) {
                    // No es una fecha válida: tratar como string normalmente
                }
            }

            cell.setCellValue(value);
            cell.setCellStyle(dataStyle);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FÁBRICA DE ESTILOS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Estilo para la fila de encabezados de tabla:
     * fondo verde #10b981, fuente blanca negrita, centrado, bordes finos.
     */
    private XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();

        // Fondo: en POI el "color de relleno" se llama ForegroundColor con patrón SOLID
        style.setFillForegroundColor(toXssfColor(ReportBrandingHelper.PDF_GREEN));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(style, ReportBrandingHelper.PDF_BORDER);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(toXssfColor(ReportBrandingHelper.PDF_WHITE));
        style.setFont(font);

        return style;
    }

    /**
     * Estilo para celdas de datos con zebra striping.
     *
     * @param isEven  {@code true} para filas pares (fondo verde pálido),
     *                {@code false} para filas impares (blanco)
     * @param isDate  {@code true} para incluir formato de fecha "DD/MM/YYYY"
     */
    private XSSFCellStyle createDataStyle(XSSFWorkbook workbook, boolean isEven, boolean isDate) {
        XSSFCellStyle style = workbook.createCellStyle();

        // Fondo alternado: par = verde muy pálido, impar = blanco
        java.awt.Color bg = isEven ? ReportBrandingHelper.PDF_EVEN_ROW : java.awt.Color.WHITE;
        style.setFillForegroundColor(toXssfColor(bg));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(style, ReportBrandingHelper.PDF_BORDER);

        // Formato de fecha Excel nativo (visible en la celda, filtrable y ordenable)
        if (isDate) {
            DataFormat dataFormat = workbook.createDataFormat();
            style.setDataFormat(dataFormat.getFormat("dd/mm/yyyy"));
        }

        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        return style;
    }

    /**
     * Estilo mínimo para el mensaje "Sin registros disponibles":
     * texto itálico en gris sutil, sin relleno.
     */
    private XSSFCellStyle createWarningStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFFont font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(toXssfColor(ReportBrandingHelper.PDF_SUBTLE_GRAY));
        style.setFont(font);

        return style;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILIDADES INTERNAS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Aplica bordes finos del color indicado en los 4 lados del estilo.
     *
     * @param style       Estilo de celda a modificar
     * @param borderColor Color AWT del borde (se convierte a XSSFColor internamente)
     */
    private void applyThinBorders(XSSFCellStyle style, java.awt.Color borderColor) {
        XSSFColor xssfBorder = toXssfColor(borderColor);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setTopBorderColor(xssfBorder);
        style.setBottomBorderColor(xssfBorder);
        style.setLeftBorderColor(xssfBorder);
        style.setRightBorderColor(xssfBorder);
    }

    /**
     * Convierte un {@link java.awt.Color} a {@link XSSFColor} para Apache POI.
     *
     * Nota sobre bytes con signo: Java trata {@code byte} como valor con signo
     * (-128..127), pero Apache POI los interpreta como unsigned al generar el XML
     * OOXML, por lo que el cast {@code (byte) 185} que Java almacena como -71
     * produce el color correcto en el archivo resultante.
     */
    private XSSFColor toXssfColor(java.awt.Color color) {
        byte[] rgb = {
            (byte) color.getRed(),
            (byte) color.getGreen(),
            (byte) color.getBlue()
        };
        return new XSSFColor(rgb, new DefaultIndexedColorMap());
    }
}
