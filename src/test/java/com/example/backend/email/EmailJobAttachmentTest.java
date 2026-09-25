package com.example.backend.email;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailJobAttachmentTest {

    @Test
    void initialWithoutAttachmentsIsEmpty() {
        EmailJob job = EmailJob.initial("a@b.com", "Subject", "Body");
        assertFalse(job.hasAttachments());
        assertTrue(job.attachments().isEmpty());
    }

    @Test
    void initialWithAttachmentsPreservesOnRetry() {
        byte[] pdf = new byte[] {1, 2, 3, 4};
        EmailAttachment attachment = new EmailAttachment(
            "ReportCard_CSV001_Term1.pdf",
            "application/pdf",
            pdf);
        EmailJob job = EmailJob.initial("parent@school.local", "Results", "Body", List.of(attachment));
        assertTrue(job.hasAttachments());
        assertEquals(1, job.attachments().size());
        assertEquals("ReportCard_CSV001_Term1.pdf", job.attachments().get(0).filename());
        assertArrayEquals(pdf, job.attachments().get(0).content());

        EmailJob retry = job.nextAttempt();
        assertEquals(1, retry.attempt());
        assertEquals(1, retry.attachments().size());
        assertArrayEquals(pdf, retry.attachments().get(0).content());
    }
}
