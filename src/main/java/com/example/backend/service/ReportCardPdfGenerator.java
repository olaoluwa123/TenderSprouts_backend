package com.example.backend.service;

import com.example.backend.dto.reportcard.ReportCardResponse;
import com.example.backend.dto.reportcard.ReportCardSubjectGrade;
import com.example.backend.dto.reportcard.ReportCardSubjectSummary;
import com.example.backend.util.PerformanceBandUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ReportCardPdfGenerator {

    private static final String LOGO_RESOURCE = "/report/tender_logo.png";
    private static final String HEAD_SIGN_RESOURCE = "/report/head_teacher_sign.png";

    private static final Font SCHOOL = new Font(Font.HELVETICA, 15, Font.BOLD, PerformanceBandUtil.HEADER_RED);
    private static final Font TITLE = new Font(Font.HELVETICA, 10, Font.BOLD, PerformanceBandUtil.HEADER_GREEN);
    private static final Font TERM = new Font(Font.HELVETICA, 9, Font.BOLD, PerformanceBandUtil.HEADER_BLUE);
    private static final Font LABEL = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font BODY = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font SMALL = new Font(Font.HELVETICA, 7, Font.BOLD);
    private static final Font SMALL_BOLD = new Font(Font.HELVETICA, 7.5f, Font.BOLD);

    public byte[] generate(ReportCardResponse card) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Narrower content column on A4; pink frame wraps the content box
            Document document = new Document(PageSize.A4, 70f, 70f, 24f, 24f);
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(headerTable(card));
            spacer(document, 2);
            document.add(pupilTable(card));
            spacer(document, 2);

            document.add(subjectsTable(card));
            spacer(document, 1);
            document.add(commentsTable(card));

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate report card PDF", ex);
        }
    }

    private PdfPTable headerTable(ReportCardResponse card) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(2);
        Image logo = loadLogo();
        if (logo != null) {
            logo.setAlignment(Image.ALIGN_CENTER);
            logoCell.addElement(logo);
        }
        table.addCell(logoCell);

        table.addCell(plain(new Phrase("TENDER SPROUTS SCHOOL", SCHOOL), Element.ALIGN_CENTER, Element.ALIGN_MIDDLE));
        table.addCell(plain(new Phrase("END OF TERM PROGRESS REPORT", TITLE), Element.ALIGN_CENTER, Element.ALIGN_MIDDLE));
        table.addCell(plain(new Phrase(termHeading(card), TERM), Element.ALIGN_CENTER, Element.ALIGN_MIDDLE));
        return table;
    }

    private Image loadLogo() {
        try (InputStream in = getClass().getResourceAsStream(LOGO_RESOURCE)) {
            if (in == null) {
                return null;
            }
            Image image = Image.getInstance(in.readAllBytes());
            image.scaleToFit(90, 70);
            image.setAlignment(Image.ALIGN_CENTER);
            return image;
        } catch (Exception ex) {
            return null;
        }
    }

    private PdfPTable pupilTable(ReportCardResponse card) {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPTable info = new PdfPTable(new float[] {9, 22, 6, 7, 6, 8, 10, 8, 12, 12});
        info.setWidthPercentage(100);
        info.setSplitLate(false);
        info.addCell(labelCell("NAME:"));
        info.addCell(valueCell(nz(card.studentName())));
        info.addCell(labelCell("Age:"));
        info.addCell(valueCell(card.ageYears() != null ? card.ageYears() + "yrs" : ""));
        info.addCell(labelCell("Sex:"));
        info.addCell(valueCell(nz(card.gender())));
        info.addCell(labelCell("Height:"));
        info.addCell(valueCell(nz(card.height())));
        info.addCell(labelCell("Weight:"));
        info.addCell(valueCell(nz(card.weight())));

        PdfPTable row2 = new PdfPTable(new float[] {58, 42});
        row2.setWidthPercentage(100);
        String classLine = nz(card.classTeacherName());
        if (!classLine.isBlank()) {
            classLine = classLine.toUpperCase(Locale.ROOT) + "   CLASS: " + nz(card.className());
        } else {
            classLine = "CLASS: " + nz(card.className());
        }
        row2.addCell(comboCell("", classLine));
        row2.addCell(comboCell("Admission Number", nz(card.admissionNumber())));

        PdfPTable row3 = new PdfPTable(3);
        row3.setWidthPercentage(100);
        row3.addCell(comboCell("No. of times school opened", num(card.daysSchoolOpened())));
        row3.addCell(comboCell("No. of times present", num(card.daysPresent())));
        row3.addCell(comboCell("Date", nz(card.reportDate())));

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setPadding(0);
        infoCell.addElement(info);
        infoCell.addElement(row2);
        infoCell.addElement(row3);
        outer.addCell(infoCell);
        return outer;
    }

    private PdfPTable subjectsTable(ReportCardResponse card) {
        PdfPTable table = new PdfPTable(new float[] {7, 34, 12, 12, 12, 23});
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setHeaderRows(1);
        table.setExtendLastRow(false);
        table.addCell(plainHeader("S/N"));
        table.addCell(plainHeader("SUBJECTS"));
        table.addCell(plainHeader("C.A 40"));
        table.addCell(plainHeader("Exam 60"));
        table.addCell(plainHeader("Total 100"));
        table.addCell(plainHeader("Remarks"));

        List<ReportCardSubjectSummary> summaries = card.subjectSummaries() != null
            ? card.subjectSummaries()
            : List.of();
        int sn = 1;
        for (ReportCardSubjectSummary summary : summaries) {
            String band = summary.performanceBand() != null
                ? summary.performanceBand()
                : PerformanceBandUtil.toBand(summary.subjectTotal());
            table.addCell(compactCell(String.valueOf(sn++), Element.ALIGN_CENTER, false));
            table.addCell(compactCell(nz(summary.subjectName()), Element.ALIGN_LEFT, false));
            table.addCell(compactCell(assessmentScore(summary.breakdown(), "CA"), Element.ALIGN_CENTER, false));
            table.addCell(compactCell(assessmentScore(summary.breakdown(), "EXAM"), Element.ALIGN_CENTER, false));
            table.addCell(compactCell(formatDecimal(summary.subjectTotal()), Element.ALIGN_CENTER, false));
            table.addCell(bandCell(band));
        }

        table.addCell(compactCell("", Element.ALIGN_CENTER, true));
        table.addCell(compactCell("Total", Element.ALIGN_LEFT, true));
        table.addCell(compactCell("", Element.ALIGN_CENTER, true));
        table.addCell(compactCell("", Element.ALIGN_CENTER, true));
        table.addCell(compactCell(formatDecimal(card.totalScore()), Element.ALIGN_CENTER, true));
        table.addCell(compactCell("", Element.ALIGN_CENTER, true));

        table.addCell(compactCell("", Element.ALIGN_CENTER, true));
        table.addCell(compactCell("Average%", Element.ALIGN_LEFT, true));
        table.addCell(compactCell("", Element.ALIGN_CENTER, true));
        table.addCell(compactCell("", Element.ALIGN_CENTER, true));
        table.addCell(compactCell(formatDecimal(card.averageScore()), Element.ALIGN_CENTER, true));
        String overallBand = card.performanceBand() != null
            ? card.performanceBand()
            : PerformanceBandUtil.toBand(card.averageScore());
        table.addCell(bandCell(overallBand));
        return table;
    }

    private PdfPTable commentsTable(ReportCardResponse card) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(commentBlock("Manipulative skill:", "", 1));
        table.addCell(commentBlock("CONDUCT:", "", 1));
        table.addCell(commentBlock("ATTITUDE TO WORK:", nz(card.attitudeComment()), 1));
        table.addCell(commentBlock("AREA(S) TO IMPROVE ON:", nz(card.areasToImprove()), 1));
        table.addCell(commentBlock("REMARKS", remark(card), 1));
        table.addCell(commentBlock("Class Educator’s Comment/Sign:", nz(card.comments()), 1));
        table.addCell(headTeacherSignBlock());
        return table;
    }

    private PdfPCell headTeacherSignBlock() {
        Phrase title = new Phrase("Head Teacher's Sign/Date:\n", SMALL_BOLD);
        PdfPCell cell = new PdfPCell();
        cell.setPadding(4);
        cell.setMinimumHeight(36);
        cell.setBorderWidth(0.5f);
        cell.addElement(title);
        Image sign = loadHeadTeacherSign();
        if (sign != null) {
            cell.addElement(sign);
        }
        return cell;
    }

    private Image loadHeadTeacherSign() {
        try (InputStream in = getClass().getResourceAsStream(HEAD_SIGN_RESOURCE)) {
            if (in == null) {
                return null;
            }
            Image image = Image.getInstance(in.readAllBytes());
            image.scaleToFit(120, 36);
            image.setAlignment(Image.ALIGN_LEFT);
            return image;
        } catch (Exception ex) {
            return null;
        }
    }

    private PdfPCell bandCell(String band) {
        String label = nz(band);
        Font font = new Font(Font.HELVETICA, 6.5f, Font.BOLD, PerformanceBandUtil.bandTextColor(band));
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setBackgroundColor(PerformanceBandUtil.bandColor(band));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(2);
        cell.setPaddingBottom(2);
        cell.setPaddingLeft(2);
        cell.setPaddingRight(2);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private String termHeading(ReportCardResponse card) {
        String term = nz(card.termName()).toUpperCase(Locale.ROOT);
        String session = nz(card.sessionName());
        if (term.isBlank() && session.isBlank()) {
            return "";
        }
        if (session.isBlank()) {
            return term;
        }
        return term + " " + session + " SESSION";
    }

    private String remark(ReportCardResponse card) {
        if (card.comments() != null && !card.comments().isBlank()) {
            return card.comments();
        }
        return nz(card.attitudeComment());
    }

    private PdfPCell commentBlock(String title, String body, int colspan) {
        Phrase phrase = new Phrase();
        phrase.add(new Phrase(title + "\n", SMALL_BOLD));
        phrase.add(new Phrase(nz(body), SMALL));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setColspan(colspan);
        cell.setPadding(4);
        cell.setMinimumHeight(26);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private PdfPCell plainHeader(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SMALL_BOLD));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private PdfPCell compactCell(String text, int align, boolean bold) {
        return compactCell(text, align, bold, 1);
    }

    private PdfPCell compactCell(String text, int align, boolean bold, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold ? SMALL_BOLD : SMALL));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setColspan(colspan);
        cell.setPaddingTop(2);
        cell.setPaddingBottom(2);
        cell.setPaddingLeft(3);
        cell.setPaddingRight(3);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, LABEL));
        cell.setBorderWidth(0.5f);
        cell.setPadding(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setNoWrap(true);
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setBorderWidth(0.5f);
        cell.setPadding(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setNoWrap(true);
        return cell;
    }

    private PdfPCell comboCell(String label, String value) {
        Phrase phrase = new Phrase();
        if (label != null && !label.isBlank()) {
            phrase.add(new Phrase(label + ": ", LABEL));
        }
        phrase.add(new Phrase(value, BODY));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorderWidth(0.5f);
        cell.setPadding(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell plain(Phrase phrase, int align, int valign) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(valign);
        cell.setPadding(3);
        return cell;
    }

    private void spacer(Document document, float height) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(height);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
        document.add(table);
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.setScale(value.scale() > 0 ? Math.min(value.scale(), 2) : 0, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString();
    }

    private String assessmentScore(List<ReportCardSubjectGrade> breakdown, String type) {
        if (breakdown == null) {
            return "";
        }
        for (ReportCardSubjectGrade grade : breakdown) {
            if (type.equals(grade.assessmentType())) {
                return formatDecimal(grade.score());
            }
        }
        return "";
    }

    private String nz(String value) {
        return value != null ? value : "";
    }

    private String num(Integer value) {
        return value != null ? String.valueOf(value) : "";
    }
}
