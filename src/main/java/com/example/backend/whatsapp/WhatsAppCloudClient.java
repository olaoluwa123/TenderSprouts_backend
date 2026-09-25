package com.example.backend.whatsapp;

import com.example.backend.config.AppWhatsAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppCloudClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudClient.class);

    private final AppWhatsAppProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WhatsAppCloudClient(AppWhatsAppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    /**
     * @return {@code true} when the job was handled (sent or intentionally skipped);
     *         {@code false} when WhatsApp was enabled and sending failed
     */
    public boolean send(WhatsAppJob job) {
        if (!properties.isEnabled()) {
            log.info(
                "WhatsApp skipped (disabled): to={} template={} params={} documents={}",
                job.toE164(),
                job.templateName(),
                job.bodyParams(),
                job.hasDocuments() ? job.documents().size() : 0);
            return true;
        }
        if (blank(properties.getAccessToken()) || blank(properties.getPhoneNumberId())) {
            log.warn(
                "WhatsApp enabled but access token or phone-number-id missing; logging job to={} template={}",
                job.toE164(),
                job.templateName());
            return true;
        }
        try {
            if (job.hasTemplate()) {
                sendTemplate(job);
            }
            if (job.hasDocuments()) {
                for (WhatsAppDocument document : job.documents()) {
                    String mediaId = uploadMedia(document);
                    sendDocument(job.toE164(), mediaId, document.filename());
                }
            }
            return true;
        } catch (Exception ex) {
            log.error(
                "Failed to send WhatsApp to={} template={} attempt={}",
                job.toE164(),
                job.templateName(),
                job.attempt(),
                ex);
            return false;
        }
    }

    private void sendTemplate(WhatsAppJob job) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", job.toE164());
        body.put("type", "template");

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", job.templateName());
        template.put("language", Map.of("code", properties.getLanguageCode()));

        if (!job.bodyParams().isEmpty()) {
            List<Map<String, String>> parameters = new ArrayList<>();
            for (String param : job.bodyParams()) {
                parameters.add(Map.of("type", "text", "text", param == null ? "" : param));
            }
            template.put("components", List.of(Map.of(
                "type", "body",
                "parameters", parameters)));
        }
        body.put("template", template);
        postJson(messagesUrl(), body);
    }

    private void sendDocument(String toE164, String mediaId, String filename) throws Exception {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", mediaId);
        document.put("filename", filename);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", toE164);
        body.put("type", "document");
        body.put("document", document);
        postJson(messagesUrl(), body);
    }

    private String uploadMedia(WhatsAppDocument document) throws Exception {
        String boundary = "----WhatsAppBoundary" + System.currentTimeMillis();
        byte[] multipart = buildMultipart(boundary, document);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(mediaUrl()))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer " + properties.getAccessToken())
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(multipart))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                "WhatsApp media upload failed status=" + response.statusCode() + " body=" + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        String id = root.path("id").asText(null);
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("WhatsApp media upload response missing id: " + response.body());
        }
        return id;
    }

    private void postJson(String url, Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + properties.getAccessToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                "WhatsApp API failed status=" + response.statusCode() + " body=" + response.body());
        }
    }

    private byte[] buildMultipart(String boundary, WhatsAppDocument document) throws Exception {
        String filePartHeader = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"" + document.filename() + "\"\r\n"
            + "Content-Type: " + document.mimeType() + "\r\n\r\n";
        String messagingPart = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"messaging_product\"\r\n\r\n"
            + "whatsapp\r\n";
        String typePart = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"type\"\r\n\r\n"
            + document.mimeType() + "\r\n";
        String closing = "--" + boundary + "--\r\n";

        byte[] headerBytes = filePartHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] messagingBytes = messagingPart.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] typeBytes = typePart.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] closingBytes = closing.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] crlf = "\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        int size = headerBytes.length + document.content().length + crlf.length
            + messagingBytes.length + typeBytes.length + closingBytes.length;
        byte[] all = new byte[size];
        int pos = 0;
        System.arraycopy(headerBytes, 0, all, pos, headerBytes.length);
        pos += headerBytes.length;
        System.arraycopy(document.content(), 0, all, pos, document.content().length);
        pos += document.content().length;
        System.arraycopy(crlf, 0, all, pos, crlf.length);
        pos += crlf.length;
        System.arraycopy(messagingBytes, 0, all, pos, messagingBytes.length);
        pos += messagingBytes.length;
        System.arraycopy(typeBytes, 0, all, pos, typeBytes.length);
        pos += typeBytes.length;
        System.arraycopy(closingBytes, 0, all, pos, closingBytes.length);
        return all;
    }

    private String messagesUrl() {
        return "https://graph.facebook.com/"
            + properties.getApiVersion()
            + "/"
            + properties.getPhoneNumberId()
            + "/messages";
    }

    private String mediaUrl() {
        return "https://graph.facebook.com/"
            + properties.getApiVersion()
            + "/"
            + properties.getPhoneNumberId()
            + "/media";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
