package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.config.SystemConfigService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config.OrganizationConfigDTO;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final SystemConfigService systemConfigService;

    public byte[] generateCorporateReport(List<String[]> tableData, String requesterName) {
        // Inicializamos el documento con márgenes adecuados (A4)
        Document document = new Document(PageSize.A4, 36, 36, 60, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Encabezado con Logo (Carga resistente a despliegues .jar)
            try {
                ClassPathResource logoResource = new ClassPathResource("images/LogoHidra.png");
                // Leemos el contenido íntegro a byte array para que el lib-openpdf pueda
                // parsear la imagen
                Image logo = Image.getInstance(logoResource.getContentAsByteArray());
                logo.scaleToFit(120, 120);
                logo.setAlignment(Element.ALIGN_RIGHT);
                document.add(logo);
            } catch (Exception e) {
                // Si el logo no está en /resources/images, registramos el log de error pero no
                // bloqueamos el PDF
                System.err
                        .println("Advertencia: No se pudo cargar el logo 'LogoHidra.png'. Detalle: " + e.getMessage());
            }

            // 2. Tipografía y Estructura (Título Principal)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Paragraph title = new Paragraph("Reporte de Operaciones Agrícolas", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            title.setSpacingAfter(5);
            document.add(title);

            // Subtítulo con Metadatos
            OrganizationConfigDTO orgConfig = systemConfigService.getOrganizationConfig();

            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY);
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            Paragraph subtitle = new Paragraph(
                    orgConfig.getOrganizationName() + "\n" +
                            "Generado el: " + dateStr + " | Solicitado por: " + requesterName,
                    subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(30);
            document.add(subtitle);

            // 3. Tabla Profesional
            int numColumns = tableData.isEmpty() ? 1 : tableData.get(0).length;
            PdfPTable table = new PdfPTable(numColumns);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            // Color Verde Oscuro Corporativo (Estilo Agrícola)
            Color headerBgColor = new Color(34, 139, 34); // Verde Bosque
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);

            // Dibujar Encabezados (Asumimos que la primera fila en la lista es el HEADER)
            if (!tableData.isEmpty()) {
                String[] headers = tableData.get(0);
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setBackgroundColor(headerBgColor);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(8);
                    table.addCell(cell);
                }
            }

            // 4. Cebreado (Zebra Striping) paramostrar datos
            Color evenRowColor = new Color(245, 245, 245); // Gris muy claro
            Color oddRowColor = Color.WHITE; // Blanco
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            // Empezamos desde `i = 1` omitiendo el header
            for (int i = 1; i < tableData.size(); i++) {
                String[] row = tableData.get(i);
                Color rowColor = (i % 2 == 0) ? evenRowColor : oddRowColor;

                for (String cellData : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(cellData != null ? cellData : "N/A", dataFont));
                    cell.setBackgroundColor(rowColor);
                    cell.setPadding(6);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setBorderColor(Color.LIGHT_GRAY);
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Error grave al intentar parsear el documento PDF de openpdf: ", e);
        }

        return out.toByteArray();
    }
}
