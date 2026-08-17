package com.example.backend.util;

import com.example.backend.dto.report.ReportDataResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportPdfGenerator {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);

    public byte[] generate(ReportDataResponse report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(report.title() != null ? report.title() : "Report", TITLE_FONT));
            document.add(new Paragraph(" "));

            List<String> columns = report.columns() != null ? report.columns() : List.of();
            int colCount = Math.max(columns.size(), 1);
            PdfPTable table = new PdfPTable(colCount);
            table.setWidthPercentage(100);
            for (String column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column, HEADER_FONT));
                table.addCell(cell);
            }
            if (report.rows() != null) {
                for (List<Object> row : report.rows()) {
                    for (int i = 0; i < colCount; i++) {
                        Object value = i < row.size() ? row.get(i) : "";
                        table.addCell(new PdfPCell(new Phrase(value == null ? "" : String.valueOf(value), BODY_FONT)));
                    }
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new UncheckedIOException(new java.io.IOException("Failed to generate PDF report", ex));
        }
    }
}
