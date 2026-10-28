package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.config.AppMailProperties;
import com.example.backend.email.EmailAttachment;
import com.example.backend.email.EmailJob;
import com.example.backend.email.EmailPublisher;
import com.example.backend.email.SmtpEmailSender;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

class EmailServiceNotifyTest {

    private final List<EmailJob> published = new ArrayList<>();
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        published.clear();
        emailService = new EmailService(new CapturingEmailPublisher(published::add));
    }

    @Test
    void sendFeesPublishedEnqueuesWellComposedJob() {
        emailService.sendFeesPublished(
            "parent@school.local",
            "Mrs Parent",
            "Ada Okonkwo",
            "JSS 1A",
            "First Term",
            "http://localhost:3000/parent/fees");

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("parent@school.local", job.to());
        assertEquals("School fees published — First Term", job.subject());
        assertTrue(job.body().contains("Ada Okonkwo"));
        assertTrue(job.body().contains("JSS 1A"));
        assertTrue(job.body().contains("http://localhost:3000/parent/fees"));
        assertTrue(job.body().contains("Warm regards"));
        assertTrue(job.attachments().isEmpty());
    }

    @Test
    void sendTermResultsPublishedIncludesPdfAttachment() {
        byte[] pdf = new byte[] {37, 80, 68, 70};
        EmailAttachment attachment = new EmailAttachment(
            "ReportCard_CSV001_First_Term.pdf",
            "application/pdf",
            pdf);

        emailService.sendTermResultsPublished(
            "parent@school.local",
            "Mrs Parent",
            "Ada Okonkwo",
            "First Term",
            "http://localhost:3000/parent/results",
            List.of(attachment));

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("Term results ready — First Term", job.subject());
        assertTrue(job.body().contains("report card PDF"));
        assertTrue(job.body().contains("http://localhost:3000/parent/results"));
        assertEquals(1, job.attachments().size());
        assertEquals("application/pdf", job.attachments().get(0).contentType());
        assertEquals(4, job.attachments().get(0).content().length);
    }

    @Test
    void sendAssessmentSheetPublishedIncludesPdfAttachment() {
        EmailAttachment attachment = samplePdf("midterm-1-term-3.pdf");

        emailService.sendAssessmentSheetPublished(
            "parent@school.local",
            "Mrs Parent",
            "Ada Okonkwo",
            "Midterm — First Term",
            "http://localhost:3000/parent/results",
            List.of(attachment));

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("Assessment results ready — Midterm — First Term", job.subject());
        assertTrue(job.body().contains("assessment PDF"));
        assertTrue(job.body().contains("Ada Okonkwo"));
        assertTrue(job.body().contains("http://localhost:3000/parent/results"));
        assertEquals(1, job.attachments().size());
        assertEquals("midterm-1-term-3.pdf", job.attachments().get(0).filename());
        assertEquals("application/pdf", job.attachments().get(0).contentType());
    }

    @Test
    void sendBehaviouralReportPublishedIncludesPdfAttachment() {
        EmailAttachment attachment = samplePdf("behavioural-1-weekly-week-2.pdf");

        emailService.sendBehaviouralReportPublished(
            "parent@school.local",
            "Mrs Parent",
            "Ada Okonkwo",
            "Week 2 — First Term",
            "http://localhost:3000/parent/results",
            List.of(attachment));

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("Behavioural report ready — Week 2 — First Term", job.subject());
        assertTrue(job.body().contains("behavioural report PDF"));
        assertTrue(job.body().contains("Ada Okonkwo"));
        assertTrue(job.body().contains("http://localhost:3000/parent/results"));
        assertEquals(1, job.attachments().size());
        assertEquals("behavioural-1-weekly-week-2.pdf", job.attachments().get(0).filename());
    }

    @Test
    void sendPreschoolReportPublishedIncludesPdfAttachment() {
        EmailAttachment attachment = samplePdf("preschool-1-term-3.pdf");

        emailService.sendPreschoolReportPublished(
            "parent@school.local",
            "Mrs Parent",
            "Ada Okonkwo",
            "First Term",
            "Month 2",
            "http://localhost:3000/parent/results",
            List.of(attachment));

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("Preschool report ready — First Term · Month 2", job.subject());
        assertTrue(job.body().contains("preschool report PDF"));
        assertTrue(job.body().contains("Ada Okonkwo"));
        assertTrue(job.body().contains("http://localhost:3000/parent/results"));
        assertEquals(1, job.attachments().size());
        assertEquals("preschool-1-term-3.pdf", job.attachments().get(0).filename());
        assertEquals("application/pdf", job.attachments().get(0).contentType());
    }

    @Test
    void sendAnnouncementPublishedEnqueuesWellComposedJob() {
        emailService.sendAnnouncementPublished(
            "parent@school.local",
            "Mrs Parent",
            "Sports Day",
            "Please arrive by 9am for the annual sports day.",
            "http://localhost:3000/parent/announcements");

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("parent@school.local", job.to());
        assertEquals("School announcement — Sports Day", job.subject());
        assertTrue(job.body().contains("Dear Mrs Parent,"));
        assertTrue(job.body().contains("A new school announcement has been posted:"));
        assertTrue(job.body().contains("Sports Day"));
        assertTrue(job.body().contains("Please arrive by 9am"));
        assertTrue(job.body().contains("http://localhost:3000/parent/announcements"));
        assertTrue(job.body().contains("Warm regards"));
        assertTrue(job.attachments().isEmpty());
    }

    @Test
    void sendAnnouncementUpdatedEnqueuesUpdatedWording() {
        emailService.sendAnnouncementUpdated(
            "parent@school.local",
            "Mrs Parent",
            "Sports Day",
            "Please arrive by 8:30am — start time changed.",
            "http://localhost:3000/parent/announcements");

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("School announcement updated — Sports Day", job.subject());
        assertTrue(job.body().contains("A school announcement has been updated:"));
        assertTrue(job.body().contains("start time changed"));
        assertTrue(job.body().contains("http://localhost:3000/parent/announcements"));
    }

    private static EmailAttachment samplePdf(String filename) {
        return new EmailAttachment(filename, "application/pdf", new byte[] {37, 80, 68, 70});
    }

    private static final class CapturingEmailPublisher extends EmailPublisher {
        private final Consumer<EmailJob> sink;

        private CapturingEmailPublisher(Consumer<EmailJob> sink) {
            super(nullProvider(), new SmtpEmailSender(nullProvider(), new AppMailProperties()), null);
            this.sink = sink;
        }

        @Override
        public void publish(EmailJob job) {
            sink.accept(job);
        }

        private static <T> ObjectProvider<T> nullProvider() {
            return new ObjectProvider<>() {
                @Override
                public T getObject() throws BeansException {
                    return null;
                }

                @Override
                public T getObject(Object... args) throws BeansException {
                    return null;
                }

                @Override
                public T getIfAvailable() throws BeansException {
                    return null;
                }

                @Override
                public T getIfUnique() throws BeansException {
                    return null;
                }
            };
        }
    }
}
