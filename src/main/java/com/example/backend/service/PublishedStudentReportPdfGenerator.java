package com.example.backend.service;

import com.example.backend.dto.parentreport.StudentAssessmentReportResponse;
import com.example.backend.dto.parentreport.StudentBehaviouralReportView;
import com.example.backend.dto.parentreport.StudentPreschoolReportResponse;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PublishedStudentReportPdfGenerator {

    private static final String LOGO_RESOURCE = "/report/tender_logo.png";
    private static final String HEAD_SIGN_RESOURCE = "/report/head_teacher_sign.png";
    private static final Font SCHOOL = new Font(Font.HELVETICA, 15, Font.BOLD, PerformanceBandUtil.HEADER_RED);
    private static final Font TITLE = new Font(Font.HELVETICA, 10, Font.BOLD, PerformanceBandUtil.HEADER_GREEN);
    private static final Font TERM = new Font(Font.HELVETICA, 9, Font.BOLD, PerformanceBandUtil.HEADER_BLUE);
    private static final Font LABEL = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font BODY = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font SMALL = new Font(Font.HELVETICA, 7, Font.BOLD);
    private static final Font SMALL_BOLD = new Font(Font.HELVETICA, 7.5f, Font.BOLD);

    public byte[] generateAssessment(StudentAssessmentReportResponse report) {
        return build(documentTitle(report.type(), report.weekNumber()), report.termName(), report.sessionName(),
            (document) -> {
                document.add(pupilTable(
                    report.studentName(),
                    report.admissionNumber(),
                    report.className(),
                    report.gender(),
                    report.ageYears(),
                    report.height(),
                    report.weight(),
                    report.classTeacherName(),
                    report.daysSchoolOpened(),
                    report.daysPresent(),
                    report.reportDate()));
                spacer(document, 6);
                document.add(assessmentTable(report));
            });
    }

    public byte[] generateBehavioural(StudentBehaviouralReportView report) {
        String title = "END_OF_TERM".equals(report.periodType())
            ? "END OF TERM BEHAVIOURAL REPORT"
            : "WEEKLY BEHAVIOURAL REPORT";
        if (report.weekNumber() != null && report.weekNumber() > 0) {
            title = title + " — WEEK " + report.weekNumber();
        }
        return build(title, report.termName(), report.sessionName(), (document) -> {
            document.add(pupilTable(
                report.studentName(),
                report.admissionNumber(),
                report.className(),
                report.gender(),
                report.ageYears(),
                report.height(),
                report.weight(),
                report.classTeacherName(),
                report.daysSchoolOpened(),
                report.daysPresent(),
                report.reportDate()));
            spacer(document, 6);
            document.add(behaviouralTable(report));
        });
    }

    public byte[] generatePreschool(StudentPreschoolReportResponse report) {
        int month = report.monthNumber() == null ? 0 : report.monthNumber();
        String period = month > 0 ? "Month " + month : "End of term";
        String termHeading = report.termName() == null || report.termName().isBlank()
            ? period
            : report.termName() + " · " + period;
        return build("PRESCHOOL REPORT", termHeading, report.sessionName(), (document) -> {
            document.add(pupilTable(
                report.studentName(),
                report.admissionNumber(),
                report.className(),
                report.gender(),
                report.ageYears(),
                report.height(),
                report.weight(),
                report.classTeacherName(),
                report.daysSchoolOpened(),
                report.daysPresent(),
                report.reportDate()));
            spacer(document, 6);
            document.add(preschoolTable(report));
            spacer(document, 6);
            document.add(preschoolFooter(report));
        });
    }

    private byte[] build(String title, String termName, String sessionName, PdfBody body) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 70f, 70f, 24f, 24f);
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(headerTable(title, termName, sessionName));
            spacer(document, 6);
            body.write(document);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate report PDF", ex);
        }
    }

    private PdfPTable headerTable(String title, String termName, String sessionName) {
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
        table.addCell(plain(new Phrase("TENDER SPROUTS SCHOOL", SCHOOL)));
        table.addCell(plain(new Phrase(title, TITLE)));
        table.addCell(plain(new Phrase(termHeading(termName, sessionName), TERM)));
        return table;
    }

    private PdfPTable pupilTable(
        String name,
        String admission,
        String className,
        String gender,
        Integer ageYears,
        String height,
        String weight,
        String classTeacherName,
        Integer daysSchoolOpened,
        Integer daysPresent,
        String reportDate) {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPTable info = new PdfPTable(new float[] {9, 22, 6, 7, 6, 8, 10, 8, 12, 12});
        info.setWidthPercentage(100);
        info.setSplitLate(false);
        info.addCell(labelCell("NAME:"));
        info.addCell(valueCell(nz(name)));
        info.addCell(labelCell("Age:"));
        info.addCell(valueCell(ageYears != null ? ageYears + "yrs" : ""));
        info.addCell(labelCell("Sex:"));
        info.addCell(valueCell(nz(gender)));
        info.addCell(labelCell("Height:"));
        info.addCell(valueCell(nz(height)));
        info.addCell(labelCell("Weight:"));
        info.addCell(valueCell(nz(weight)));

        PdfPTable row2 = new PdfPTable(new float[] {58, 42});
        row2.setWidthPercentage(100);
        String classLine = nz(classTeacherName);
        if (!classLine.isBlank()) {
            classLine = classLine.toUpperCase(Locale.ROOT) + "   CLASS: " + nz(className);
        } else {
            classLine = "CLASS: " + nz(className);
        }
        row2.addCell(comboCell("", classLine));
        row2.addCell(comboCell("Admission Number", nz(admission)));

        PdfPTable row3 = new PdfPTable(3);
        row3.setWidthPercentage(100);
        row3.addCell(comboCell("No. of times school opened", num(daysSchoolOpened)));
        row3.addCell(comboCell("No. of times present", num(daysPresent)));
        row3.addCell(comboCell("Date", nz(reportDate)));

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setPadding(0);
        infoCell.addElement(info);
        infoCell.addElement(row2);
        infoCell.addElement(row3);
        outer.addCell(infoCell);
        return outer;
    }

    private PdfPTable assessmentTable(StudentAssessmentReportResponse report) {
        List<StudentAssessmentReportResponse.ScoreType> types = report.types() != null
            ? report.types()
            : List.of();
        boolean midterm = "MIDTERM".equals(report.type());
        int extra = midterm ? 1 : 0;
        int cols = 2 + types.size() + extra;
        float[] widths = new float[cols];
        widths[0] = 8;
        widths[1] = midterm ? 28 : 36;
        float rest = midterm ? 64f : 56f;
        int scoreCols = types.size() + extra;
        for (int i = 2; i < cols; i++) {
            widths[i] = rest / Math.max(scoreCols, 1);
        }
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.addCell(headerCell("S/N"));
        table.addCell(headerCell("SUBJECT"));
        for (StudentAssessmentReportResponse.ScoreType type : types) {
            table.addCell(headerCell(scoreTypeHeader(type.name(), type.maxScore())));
        }
        if (midterm) {
            BigDecimal maxTotal = types.stream()
                .map(StudentAssessmentReportResponse.ScoreType::maxScore)
                .filter(max -> max != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            table.addCell(headerCell(scoreTypeHeader("Total",
                maxTotal.compareTo(BigDecimal.ZERO) > 0 ? maxTotal : null)));
        }
        int sn = 1;
        for (StudentAssessmentReportResponse.SubjectRow subject : report.subjects() != null
            ? report.subjects() : List.<StudentAssessmentReportResponse.SubjectRow>of()) {
            table.addCell(bodyCell(String.valueOf(sn++), Element.ALIGN_CENTER));
            table.addCell(bodyCell(nz(subject.name()), Element.ALIGN_LEFT));
            for (StudentAssessmentReportResponse.ScoreType type : types) {
                BigDecimal score = subject.scores() != null ? subject.scores().get(type.code()) : null;
                table.addCell(bodyCell(formatDecimal(score), Element.ALIGN_CENTER));
            }
            if (midterm) {
                table.addCell(bodyCell(
                    formatDecimal(ParentPublishedReportService.subjectTotal(subject, types)),
                    Element.ALIGN_CENTER));
            }
        }
        if (("WEEKLY_TEST".equals(report.type()) || midterm) && report.averageScore() != null) {
            table.addCell(bodyCell("", Element.ALIGN_CENTER));
            table.addCell(headerCell("Average%"));
            if (midterm) {
                for (int i = 0; i < types.size(); i++) {
                    table.addCell(bodyCell("", Element.ALIGN_CENTER));
                }
                table.addCell(headerCell(formatDecimal(report.averageScore()) + "%"));
            } else {
                List<StudentAssessmentReportResponse.SubjectRow> subjects = report.subjects() != null
                    ? report.subjects()
                    : List.of();
                for (StudentAssessmentReportResponse.ScoreType type : types) {
                    BigDecimal percent = typeAveragePercent(subjects, type.code(), type.maxScore());
                    table.addCell(headerCell(percent == null ? "" : formatDecimal(percent) + "%"));
                }
            }
        }
        return table;
    }

    private PdfPTable behaviouralTable(StudentBehaviouralReportView report) {
        PdfPTable table = new PdfPTable(new float[] {40, 60});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.addCell(headerCell("TRAIT"));
        table.addCell(headerCell("RATING"));
        addTrait(table, "Punctuality", report.punctuality());
        addTrait(table, "Neatness", report.neatness());
        addTrait(table, "Cooperation", report.cooperation());
        addTrait(table, "Responsibility", report.responsibility());
        addTrait(table, "Conduct", report.conduct());
        table.addCell(headerCell("REMARKS"));
        table.addCell(bodyCell(nz(report.remarks()), Element.ALIGN_LEFT));
        return table;
    }

    private void addTrait(PdfPTable table, String trait, String rating) {
        table.addCell(bodyCell(trait, Element.ALIGN_LEFT));
        table.addCell(bodyCell(nz(rating), Element.ALIGN_CENTER));
    }

    private PdfPTable preschoolTable(StudentPreschoolReportResponse report) {
        PdfPTable table = new PdfPTable(new float[] {28, 72});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.addCell(headerCell("SUBJECT"));
        table.addCell(headerCell("COMMENT"));
        List<StudentPreschoolReportResponse.SubjectComment> subjects = report.subjects() != null
            ? report.subjects()
            : List.of();
        for (StudentPreschoolReportResponse.SubjectComment subject : subjects) {
            table.addCell(bodyCell(nz(subject.name()), Element.ALIGN_LEFT));
            table.addCell(bodyCell(nz(subject.comment()), Element.ALIGN_LEFT));
        }
        return table;
    }

    private PdfPTable preschoolFooter(StudentPreschoolReportResponse report) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(commentBlock("Class Teacher's Comment:", nz(report.classTeacherComment())));
        table.addCell(commentBlock("Head Teacher's Remark:", nz(report.headTeacherRemark())));
        PdfPCell sign = new PdfPCell();
        sign.setPadding(4);
        sign.setMinimumHeight(36);
        sign.setBorderWidth(0.5f);
        sign.addElement(new Phrase("Date/Sign: " + nz(report.reportDate()) + "\n", SMALL_BOLD));
        Image image = loadHeadTeacherSign();
        if (image != null) {
            sign.addElement(image);
        }
        table.addCell(sign);
        return table;
    }

    private PdfPCell commentBlock(String title, String body) {
        Phrase phrase = new Phrase();
        phrase.add(new Phrase(title + "\n", SMALL_BOLD));
        phrase.add(new Phrase(body, SMALL));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setPadding(4);
        cell.setMinimumHeight(28);
        cell.setBorderWidth(0.5f);
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

    private String documentTitle(String type, Integer weekNumber) {
        if ("MIDTERM".equals(type)) {
            return "MIDTERM PROGRESS REPORT";
        }
        if (weekNumber != null && weekNumber > 0) {
            return "WEEKLY TEST RESULT — WEEK " + weekNumber;
        }
        return "WEEKLY TEST RESULT";
    }

    private String termHeading(String termName, String sessionName) {
        String term = nz(termName).toUpperCase(Locale.ROOT);
        String session = nz(sessionName);
        if (term.isBlank() && session.isBlank()) {
            return "";
        }
        if (session.isBlank()) {
            return term;
        }
        return term + " " + session + " SESSION";
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

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SMALL_BOLD));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private PdfPCell bodyCell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SMALL));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
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

    private String num(Integer value) {
        return value != null ? String.valueOf(value) : "";
    }

    private PdfPCell plain(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
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

    private BigDecimal typeAveragePercent(
        List<StudentAssessmentReportResponse.SubjectRow> subjects,
        String typeCode,
        BigDecimal maxScore) {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        List<BigDecimal> percents = new ArrayList<>();
        for (StudentAssessmentReportResponse.SubjectRow subject : subjects) {
            BigDecimal score = subject.scores() != null ? subject.scores().get(typeCode) : null;
            if (score != null) {
                percents.add(score.multiply(new BigDecimal("100")).divide(maxScore, 2, RoundingMode.HALF_UP));
            }
        }
        if (percents.isEmpty()) {
            return null;
        }
        BigDecimal sum = percents.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(percents.size()), 2, RoundingMode.HALF_UP);
    }

    private String scoreTypeHeader(String name, BigDecimal maxScore) {
        String label = nz(name)
            .replaceFirst("\\s*\\(\\s*[\\d.]+\\s*\\)\\s*$", "")
            .replaceFirst("/\\s*[\\d.]+\\s*$", "")
            .trim();
        String max = formatDecimal(maxScore);
        if (label.isBlank()) {
            return max.isBlank() ? "" : "(" + max + ")";
        }
        return max.isBlank() ? label : label + " (" + max + ")";
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.setScale(value.scale() > 0 ? Math.min(value.scale(), 2) : 0, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString();
    }

    private String nz(String value) {
        return value != null ? value : "";
    }

    @FunctionalInterface
    private interface PdfBody {
        void write(Document document) throws Exception;
    }
}
