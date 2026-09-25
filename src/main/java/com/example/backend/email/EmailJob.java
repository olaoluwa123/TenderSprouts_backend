package com.example.backend.email;

import java.util.List;

/**
 * Outbound email payload for the RabbitMQ email pipeline.
 *
 * @param attempt zero-based send attempt; retries run while {@code attempt < MAX_RETRY_ATTEMPTS}
 */
public record EmailJob(
    String to,
    String subject,
    String body,
    int attempt,
    List<EmailAttachment> attachments
) {
    public static final int MAX_RETRY_ATTEMPTS = 5;

    public EmailJob {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Email to is required");
        }
        if (subject == null) {
            subject = "";
        }
        if (body == null) {
            body = "";
        }
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must be >= 0");
        }
        if (attachments == null) {
            attachments = List.of();
        } else {
            attachments = List.copyOf(attachments);
        }
    }

    public static EmailJob initial(String to, String subject, String body) {
        return new EmailJob(to, subject, body, 0, List.of());
    }

    public static EmailJob initial(
        String to,
        String subject,
        String body,
        List<EmailAttachment> attachments) {
        return new EmailJob(to, subject, body, 0, attachments == null ? List.of() : attachments);
    }

    public EmailJob nextAttempt() {
        return new EmailJob(to, subject, body, attempt + 1, attachments);
    }

    public boolean canRetry() {
        return attempt < MAX_RETRY_ATTEMPTS;
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
}
