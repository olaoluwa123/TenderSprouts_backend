package com.example.backend.service;

import com.example.backend.config.PaystackProperties;
import com.example.backend.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class PaystackClient {

    private final PaystackProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PaystackClient(PaystackProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .baseUrl("https://api.paystack.co")
            .build();
    }

    public JsonNode initializeTransaction(String email, long amountKobo, String reference, Map<String, Object> metadata) {
        requireConfigured();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("amount", amountKobo);
        payload.put("currency", "NGN");
        payload.put("reference", reference);
        if (metadata != null && !metadata.isEmpty()) {
            payload.put("metadata", metadata);
        }
        try {
            String body = restClient.post()
                .uri("/transaction/initialize")
                .header("Authorization", "Bearer " + properties.getSecretKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("status").asBoolean(false)) {
                throw new BadRequestException(root.path("message").asText("Paystack initialize failed"));
            }
            return root.path("data");
        } catch (BadRequestException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException(paystackErrorMessage(ex.getResponseBodyAsString(),
                "Paystack initialize failed: " + ex.getStatusCode().value()));
        } catch (Exception ex) {
            throw new BadRequestException("Paystack initialize failed");
        }
    }

    public JsonNode verifyTransaction(String reference) {
        requireConfigured();
        try {
            String body = restClient.get()
                .uri("/transaction/verify/{reference}", reference)
                .header("Authorization", "Bearer " + properties.getSecretKey().trim())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
            return objectMapper.readTree(body);
        } catch (RestClientResponseException ex) {
            throw new BadRequestException(paystackErrorMessage(ex.getResponseBodyAsString(),
                "Paystack verification failed: " + ex.getStatusCode().value()));
        } catch (Exception ex) {
            throw new BadRequestException("Paystack verification failed");
        }
    }

    public boolean isValidWebhookSignature(String signature, String rawBody) {
        if (!properties.isConfigured() || signature == null || signature.isBlank() || rawBody == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.getSecretKey().trim().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computed = toHex(hash);
            return computed.equalsIgnoreCase(signature.trim());
        } catch (Exception ex) {
            return false;
        }
    }

    public JsonNode parseJson(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid Paystack webhook payload");
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new BadRequestException("Paystack is not configured. Set PAYSTACK_SECRET_KEY and PAYSTACK_PUBLIC_KEY.");
        }
    }

    private String paystackErrorMessage(String responseBody, String fallback) {
        if (responseBody == null || responseBody.isBlank()) {
            return fallback;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("message").asText(null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return fallback;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
