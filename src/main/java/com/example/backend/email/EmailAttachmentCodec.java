package com.example.backend.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EmailAttachmentCodec {

    private static final Logger log = LoggerFactory.getLogger(EmailAttachmentCodec.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EmailAttachmentCodec() {
    }

    static String toJson(List<EmailAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (EmailAttachment attachment : attachments) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("filename", attachment.filename());
            row.put("contentType", attachment.contentType());
            row.put("contentBase64", Base64.getEncoder().encodeToString(attachment.content()));
            rows.add(row);
        }
        try {
            return MAPPER.writeValueAsString(rows);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize email attachments: {}", ex.getMessage());
            return null;
        }
    }

    static List<EmailAttachment> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, String>> rows = MAPPER.readValue(json, new TypeReference<>() {
            });
            List<EmailAttachment> attachments = new ArrayList<>();
            for (Map<String, String> row : rows) {
                String filename = row.getOrDefault("filename", "attachment.bin");
                String contentType = row.getOrDefault("contentType", "application/octet-stream");
                byte[] content = Base64.getDecoder().decode(row.getOrDefault("contentBase64", ""));
                attachments.add(new EmailAttachment(filename, contentType, content));
            }
            return attachments;
        } catch (Exception ex) {
            log.warn("Failed to deserialize email attachments: {}", ex.getMessage());
            return List.of();
        }
    }
}
