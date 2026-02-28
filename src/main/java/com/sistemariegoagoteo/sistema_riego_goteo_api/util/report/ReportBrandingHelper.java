package com.sistemariegoagoteo.sistema_riego_goteo_api.util.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad centralizada de marca corporativa para reportes PDF y Excel.
 *
 * Responsabilidades:
 * - Carga del logo desde el classpath (con fallback seguro si no existe).
 * - Construcción del membrete PDF (logo izquierda, textos derecha).
 * - Pie de página PDF numerado con técnica two-pass (PdfTemplate).
 * - Construcción del membrete Excel (logo embebido + texto estilizado).
 *
 * Todos los colores mantienen paridad con la paleta del frontend (#10b981).
 */
@Slf4j
@Component
public class ReportBrandingHelper {

    // ── Identidad corporativa ────────────────────────────────────────────────

    public static final String COMPANY_NAME = "Sistema de Gestión de Riego - Hidra";
    private static final String LOGO_CLASSPATH = "images/LogoHidra.png";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Paleta de colores (pública para uso en servicios hijos) ─────────────

    /** Verde esmeralda principal: #10b981 */
    public static final Color PDF_GREEN = new Color(16, 185, 129);
    /** Texto sobre fondo verde */
    public static final Color PDF_WHITE = Color.WHITE;
    /** Filas pares — verde muy pálido */
    public static final Color PDF_EVEN_ROW = new Color(240, 253, 244);
    /** Borde de celdas */
    public static final Color PDF_BORDER = new Color(209, 250, 229);
    /** Metadatos y pie de página */
    public static final Color PDF_SUBTLE_GRAY = new Color(107, 114, 128);
    /** Título principal */
    public static final Color PDF_DARK = new Color(31, 41, 55);

    // ── Dimensiones del logo en PDF ──────────────────────────────────────────

    private static final float PDF_LOGO_W = 110f;
    private static final float PDF_LOGO_H = 55f;

    // ════════════════════════════════════════════════════════════════════════
    // CARGA DE LOGO (compartida por PDF y Excel)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Lee el logo corporativo desde el classpath.
     * Si el recurso no existe o falla la lectura, registra un aviso y retorna
     * {@code null}; el llamador debe continuar la generación sin imagen.
     */
    private byte[] loadLogoBytes() {
        try {
            ClassPathResource resource = new ClassPathResource(LOGO_CLASSPATH);
            if (!resource.exists()) {
                log.warn("[ReportBrandingHelper] Logo no encontrado en classpath: '{}'. "
                        + "El reporte se generará sin imagen corporativa.", LOGO_CLASSPATH);
                return null;
            }
            return resource.getContentAsByteArray();
        } catch (IOException e) {
            log.warn("[ReportBrandingHelper] Error al leer '{}': {}. "
                    + "El reporte se generará sin imagen corporativa.",
                    LOGO_CLASSPATH, e.getMessage());
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MEMBRETE PDF
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Inserta el membrete corporativo completo en el documento PDF.
     *
     * Diseño: tabla 2 columnas — [Logo | Empresa / Título / Fecha / Solicitante]
     * Seguida de una línea separadora verde.
     *
     * @param document      Documento OpenPDF ya abierto ({@code document.open()}
     *                      previo)
     * @param reportTitle   Título visible del reporte (ej. "Reporte de Tareas")
     * @param requesterName Nombre del usuario que solicita el reporte
     * @throws DocumentException si OpenPDF no puede añadir los elementos
     */
    public void buildPdfHeader(Document document, String reportTitle, String requesterName)
            throws DocumentException {

        // Tabla 2 columnas: 30% logo / 70% texto
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100f);
        header.setWidths(new float[] { 3f, 7f });
        header.setSpacingAfter(10f);

        // Celda del logo (izquierda)
        PdfPCell logoCell = buildLogoCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.addCell(logoCell);

        // Celda de texto (derecha)
        PdfPCell textCell = buildTextCell(reportTitle, requesterName);
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.addCell(textCell);

        document.add(header);
        document.add(buildDividerLine());
        document.add(Chunk.NEWLINE);
    }

    /** Celda izquierda del membrete: contiene el logo escalado. */
    private PdfPCell buildLogoCell() {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6f);

        byte[] bytes = loadLogoBytes();
        if (bytes != null) {
            try {
                Image logo = Image.getInstance(bytes);
                logo.scaleToFit(PDF_LOGO_W, PDF_LOGO_H);
                cell.addElement(logo);
                return cell;
            } catch (BadElementException | IOException e) {
                log.warn("[ReportBrandingHelper] Imagen del logo no es válida: {}", e.getMessage());
            }
        }
        // Fallback: celda vacía sin interrumpir el flujo
        cell.addElement(new Phrase(""));
        return cell;
    }

    /**
     * Celda derecha del membrete: empresa, título del reporte, fecha y solicitante.
     */
    private PdfPCell buildTextCell(String reportTitle, String requesterName) {
        com.lowagie.text.Font fEmpresa = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, PDF_DARK);
        com.lowagie.text.Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, PDF_GREEN);
        com.lowagie.text.Font fMeta = FontFactory.getFont(FontFactory.HELVETICA, 9f, PDF_SUBTLE_GRAY);

        Paragraph empresa = new Paragraph(COMPANY_NAME, fEmpresa);
        empresa.setAlignment(Element.ALIGN_RIGHT);

        Paragraph titulo = new Paragraph(reportTitle, fTitulo);
        titulo.setAlignment(Element.ALIGN_RIGHT);
        titulo.setSpacingBefore(3f);

        String fecha = LocalDateTime.now().format(DATE_FMT);
        Paragraph meta = new Paragraph(
                "Generado el: " + fecha + "   |   Por: " + requesterName, fMeta);
        meta.setAlignment(Element.ALIGN_RIGHT);
        meta.setSpacingBefore(5f);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(6f);
        cell.addElement(empresa);
        cell.addElement(titulo);
        cell.addElement(meta);
        return cell;
    }

    /** Línea separadora verde de 2pt bajo el membrete. */
    private PdfPTable buildDividerLine() throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100f);

        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthLeft(0f);
        cell.setBorderWidthRight(0f);
        cell.setBorderWidthBottom(2f);
        cell.setBorderColorBottom(PDF_GREEN);
        cell.setPaddingBottom(4f);
        line.addCell(cell);
        return line;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PIE DE PÁGINA PDF — "Página X de Y"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Evento de página OpenPDF que dibuja "Página X de Y" en el margen inferior.
     *
     * Técnica two-pass con {@link PdfTemplate}:
     * <ol>
     * <li>{@code onOpenDocument}: reserva un rectángulo vacío (placeholder) para el
     * total.</li>
     * <li>{@code onEndPage}: dibuja el prefijo "Página N de " y adjunta el
     * placeholder.</li>
     * <li>{@code onCloseDocument}: rellena el placeholder con el total real de
     * páginas.</li>
     * </ol>
     *
     * Registro en el escritor:
     * 
     * <pre>
     * PdfWriter writer = PdfWriter.getInstance(document, outputStream);
     * writer.setPageEvent(new ReportBrandingHelper.PdfPageNumberFooter());
     * document.open();
     * </pre>
     */
    @Slf4j
    public static class PdfPageNumberFooter extends PdfPageEventHelper {

        /**
         * Placeholder gráfico para el número total de páginas (se rellena al cerrar).
         */
        private PdfTemplate totalPagesPlaceholder;

        /** Fuente con soporte de tildes y ñ (CP1252). */
        private BaseFont font;

        private static final float FONT_SIZE = 8.5f;
        private static final float FOOTER_Y = 18f; // puntos desde el borde inferior

        public PdfPageNumberFooter() {
            try {
                font = BaseFont.createFont(
                        BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                log.warn("[PdfPageNumberFooter] No se pudo inicializar la fuente del pie: {}",
                        e.getMessage());
            }
        }

        /** Paso 1: reservar el espacio para el número total al abrir el documento. */
        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            // 30 × 12 puntos es suficiente para un número de 4 dígitos
            totalPagesPlaceholder = writer.getDirectContent().createTemplate(30f, 12f);
        }

        /** Paso 2: en cada página dibujar "Página N de [placeholder]". */
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (font == null)
                return;

            PdfContentByte canvas = writer.getDirectContent();
            float pageWidth = document.getPageSize().getWidth();
            String prefix = "Página " + writer.getPageNumber() + " de ";
            float prefixWidth = font.getWidthPoint(prefix, FONT_SIZE);
            float totalBlockWidth = prefixWidth + 30f; // 30 = ancho del placeholder
            float startX = (pageWidth - totalBlockWidth) / 2f;

            // Dibujar el texto del prefijo
            canvas.beginText();
            canvas.setFontAndSize(font, FONT_SIZE);
            canvas.setColorFill(new Color(107, 114, 128)); // PDF_SUBTLE_GRAY
            canvas.setTextMatrix(startX, FOOTER_Y);
            canvas.showText(prefix);
            canvas.endText();

            // Adjuntar el placeholder (se llenará en onCloseDocument)
            canvas.addTemplate(totalPagesPlaceholder, startX + prefixWidth, FOOTER_Y);
        }

        /**
         * Paso 3: al cerrar el documento, escribir el total real en el placeholder.
         * En OpenPDF, {@code writer.getPageNumber()} en este callback vale total + 1
         * (ya se incrementó para la página siguiente que nunca se creó).
         */
        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPagesPlaceholder == null || font == null)
                return;

            String total = String.valueOf(writer.getPageNumber() - 1);
            totalPagesPlaceholder.beginText();
            totalPagesPlaceholder.setFontAndSize(font, FONT_SIZE);
            totalPagesPlaceholder.setColorFill(new Color(107, 114, 128));
            totalPagesPlaceholder.showText(total);
            totalPagesPlaceholder.endText();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MEMBRETE EXCEL
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Inserta el membrete corporativo en la hoja Excel.
     *
     * Estructura en la hoja (filas 0–2):
     * 
     * <pre>
     *   Col 0–1 (filas 0–2) : Logo PNG embebido
     *   Col 2–6 (fila 0)    : Nombre de la empresa
     *   Col 2–6 (fila 1)    : Título del reporte
     *   Col 2–6 (fila 2)    : Fecha de generación
     * </pre>
     * 
     * Fila 3: separador vacío.
     *
     * @param workbook    Libro de trabajo XSSF activo
     * @param sheet       Hoja destino
     * @param reportTitle Título del reporte
     * @return Índice de la primera fila libre para comenzar la tabla de datos
     */
    public int buildExcelHeader(XSSFWorkbook workbook, XSSFSheet sheet, String reportTitle) {

        final int LAST_HEADER_ROW = 2; // filas 0, 1, 2 → membrete

        // Crear filas del membrete con alturas adecuadas
        sheet.createRow(0).setHeightInPoints(55f); // fila del logo: más alta
        sheet.createRow(1).setHeightInPoints(22f);
        sheet.createRow(2).setHeightInPoints(18f);

        // Insertar imagen
        insertExcelLogo(workbook, sheet);

        // Insertar texto corporativo (empresa, título, fecha)
        insertExcelBrandText(workbook, sheet, reportTitle);

        // Fila separadora entre membrete y encabezados de tabla
        int separatorRow = LAST_HEADER_ROW + 1;
        sheet.createRow(separatorRow).setHeightInPoints(6f);

        // Los encabezados de la tabla empiezan en la fila siguiente
        return separatorRow + 1;
    }

    /** Inserta el logo PNG en el área col 0–1 / fila 0–2 de la hoja. */
    private void insertExcelLogo(XSSFWorkbook workbook, XSSFSheet sheet) {
        byte[] bytes = loadLogoBytes();
        if (bytes == null)
            return; // sin logo: continuar sin interrumpir

        try {
            int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);

            XSSFCreationHelper helper = workbook.getCreationHelper();
            XSSFClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(0);
            anchor.setRow1(0);
            anchor.setCol2(2);
            anchor.setRow2(3); // ancla hasta col 2, fila 3 (excluida)
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);

            sheet.createDrawingPatriarch().createPicture(anchor, pictureIdx);
        } catch (Exception e) {
            log.warn("[ReportBrandingHelper] No se pudo insertar el logo en Excel: {}",
                    e.getMessage());
        }
    }

    /** Escribe empresa (fila 0), título (fila 1) y fecha (fila 2) en cols 2–6. */
    private void insertExcelBrandText(XSSFWorkbook workbook, XSSFSheet sheet, String reportTitle) {

        // Combinar columnas 2–6 en cada fila de texto
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 6));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 6));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 6));

        String fecha = "Generado el: " + LocalDateTime.now().format(DATE_FMT);

        // Empresa: negrita gris oscuro 12pt
        writeExcelBrandCell(workbook, sheet, 0, 2, COMPANY_NAME,
                (short) 12, true, new byte[] { (byte) 31, (byte) 41, (byte) 55 });

        // Título: negrita verde 14pt
        writeExcelBrandCell(workbook, sheet, 1, 2, reportTitle,
                (short) 14, true, new byte[] { (byte) 16, (byte) 185, (byte) 129 });

        // Fecha: normal gris sutil 9pt
        writeExcelBrandCell(workbook, sheet, 2, 2, fecha,
                (short) 9, false, new byte[] { (byte) 107, (byte) 114, (byte) 128 });
    }

    /**
     * Crea (o recupera) la celda en (rowIdx, colIdx), asigna el valor
     * y aplica el estilo de texto del membrete con alineación a la derecha.
     *
     * @param rgbColor Array de 3 bytes {R, G, B}; Java los trata como unsigned en
     *                 POI
     */
    private void writeExcelBrandCell(XSSFWorkbook workbook, XSSFSheet sheet,
            int rowIdx, int colIdx, String value,
            short fontSize, boolean bold, byte[] rgbColor) {

        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFFont font = workbook.createFont();
        font.setBold(bold);
        font.setFontHeightInPoints(fontSize);
        font.setColor(new XSSFColor(rgbColor, new DefaultIndexedColorMap()));
        style.setFont(font);

        org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIdx);
        if (row == null)
            row = sheet.createRow(rowIdx);
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(colIdx);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
