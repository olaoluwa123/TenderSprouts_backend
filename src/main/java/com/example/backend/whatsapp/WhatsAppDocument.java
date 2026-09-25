package com.example.backend.whatsapp;

public record WhatsAppDocument(
    String filename,
    String mimeType,
    byte[] content
) {
    public WhatsAppDocument {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Document filename is required");
        }
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }
        if (content == null) {
            content = new byte[0];
        }
    }
}
