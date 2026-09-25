package com.example.backend.whatsapp;

import java.util.List;

/**
 * Outbound WhatsApp payload for the RabbitMQ WhatsApp pipeline.
 *
 * @param attempt zero-based send attempt; retries run while {@code attempt < MAX_RETRY_ATTEMPTS}
 */
public record WhatsAppJob(
    String toE164,
    String templateName,
    List<String> bodyParams,
    int attempt,
    List<WhatsAppDocument> documents
) {
    public static final int MAX_RETRY_ATTEMPTS = 5;

    public WhatsAppJob {
        if (toE164 == null || toE164.isBlank()) {
            throw new IllegalArgumentException("WhatsApp toE164 is required");
        }
        if (templateName == null) {
            templateName = "";
        }
        if (bodyParams == null) {
            bodyParams = List.of();
        } else {
            bodyParams = List.copyOf(bodyParams);
        }
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must be >= 0");
        }
        if (documents == null) {
            documents = List.of();
        } else {
            documents = List.copyOf(documents);
        }
    }

    public static WhatsAppJob initial(String toE164, String templateName, List<String> bodyParams) {
        return new WhatsAppJob(toE164, templateName, bodyParams, 0, List.of());
    }

    public static WhatsAppJob initial(
        String toE164,
        String templateName,
        List<String> bodyParams,
        List<WhatsAppDocument> documents) {
        return new WhatsAppJob(
            toE164,
            templateName,
            bodyParams,
            0,
            documents == null ? List.of() : documents);
    }

    public WhatsAppJob nextAttempt() {
        return new WhatsAppJob(toE164, templateName, bodyParams, attempt + 1, documents);
    }

    public boolean canRetry() {
        return attempt < MAX_RETRY_ATTEMPTS;
    }

    public boolean hasDocuments() {
        return documents != null && !documents.isEmpty();
    }

    public boolean hasTemplate() {
        return templateName != null && !templateName.isBlank();
    }
}
