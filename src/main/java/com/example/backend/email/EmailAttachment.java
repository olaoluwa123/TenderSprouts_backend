package com.example.backend.email;

public record EmailAttachment(
    String filename,
    String contentType,
    byte[] content
) {
    public EmailAttachment {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Attachment filename is required");
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        if (content == null) {
            content = new byte[0];
        }
    }
}
