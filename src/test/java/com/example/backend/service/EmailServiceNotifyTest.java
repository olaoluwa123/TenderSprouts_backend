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

    private static final class CapturingEmailPublisher extends EmailPublisher {
        private final Consumer<EmailJob> sink;

        private CapturingEmailPublisher(Consumer<EmailJob> sink) {
            super(nullProvider(), new SmtpEmailSender(nullProvider(), new AppMailProperties()));
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
