package com.example.backend.service;

import com.example.backend.dto.reportcard.ReportCardResponse;
import com.example.backend.dto.reportcard.ReportCardSubjectGrade;
import com.example.backend.dto.reportcard.ReportCardSubjectSummary;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportCardPdfGenerator {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADING_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11);

    public byte[] generate(ReportCardResponse card) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("School Report Card", TITLE_FONT));
            document.add(new Paragraph(" "));
            document.add(line("Student", card.studentName() + " (" + card.admissionNumber() + ")"));
            document.add(line("Class", card.className()));
            document.add(line("Session", card.sessionName()));
            document.add(line("Term", card.termName()));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Summary", HEADING_FONT));
            document.add(line("Average score", formatDecimal(card.averageScore())));
            document.add(line("Overall grade", card.letterGrade() != null ? card.letterGrade() : "—"));
            document.add(line("Rank in class", card.rankInClass() != null ? card.rankInClass().toString() : "—"));
            if (card.comments() != null && !card.comments().isBlank()) {
                document.add(line("Comments", card.comments()));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Subject Grades", HEADING_FONT));
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Subject"));
            table.addCell(headerCell("CA"));
            table.addCell(headerCell("Exam"));
            table.addCell(headerCell("Total"));
            table.addCell(headerCell("Grade"));
            List<ReportCardSubjectSummary> summaries = card.subjectSummaries() != null
                ? card.subjectSummaries()
                : List.of();
            if (!summaries.isEmpty()) {
                for (ReportCardSubjectSummary summary : summaries) {
                    table.addCell(bodyCell(summary.subjectName()));
                    table.addCell(bodyCell(assessmentScore(summary.breakdown(), "CA")));
                    table.addCell(bodyCell(assessmentScore(summary.breakdown(), "EXAM")));
                    table.addCell(bodyCell(formatDecimal(summary.subjectTotal())));
                    table.addCell(bodyCell(summary.letterGrade()));
                }
            } else {
                for (ReportCardSubjectGrade grade : card.subjectGrades()) {
                    table.addCell(bodyCell(grade.subjectName()));
                    table.addCell(bodyCell("CA".equals(grade.assessmentType())
                        ? formatDecimal(grade.score()) : "—"));
                    table.addCell(bodyCell("EXAM".equals(grade.assessmentType())
                        ? formatDecimal(grade.score()) : "—"));
                    table.addCell(bodyCell("—"));
                    table.addCell(bodyCell("—"));
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate report card PDF", ex);
        }
    }

    private Paragraph line(String label, String value) {
        return new Paragraph(label + ": " + value, BODY_FONT);
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADING_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell bodyCell(String text) {
        return new PdfPCell(new Phrase(text != null ? text : "—", BODY_FONT));
    }

    private String formatDecimal(BigDecimal value) {
        return value != null ? value.toPlainString() : "—";
    }

    private String assessmentScore(List<ReportCardSubjectGrade> breakdown, String type) {
        if (breakdown == null) {
            return "—";
        }
        for (ReportCardSubjectGrade grade : breakdown) {
            if (type.equals(grade.assessmentType())) {
                return formatDecimal(grade.score());
            }
        }
        return "—";
    }
}
