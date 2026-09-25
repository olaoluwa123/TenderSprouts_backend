package com.example.backend.service;

import com.example.backend.config.AppWhatsAppProperties;
import com.example.backend.whatsapp.WhatsAppDocument;
import com.example.backend.whatsapp.WhatsAppJob;
import com.example.backend.whatsapp.WhatsAppPublisher;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    private final WhatsAppPublisher whatsAppPublisher;
    private final AppWhatsAppProperties properties;

    public WhatsAppService(WhatsAppPublisher whatsAppPublisher, AppWhatsAppProperties properties) {
        this.whatsAppPublisher = whatsAppPublisher;
        this.properties = properties;
    }

    public void sendWelcomeCredentials(
        String toE164,
        String recipientName,
        String roleLabel,
        String email,
        String temporaryPassword,
        String loginUrl) {
        String name = blankToDefault(recipientName, "parent");
        String role = blankToDefault(roleLabel, "parent");
        enqueue(
            toE164,
            properties.getTemplates().getWelcome(),
            List.of(name, role, blankToDefault(email, ""), blankToDefault(temporaryPassword, ""), blankToDefault(loginUrl, "")));
    }

    public void sendPasswordReset(String toE164, String recipientName, String resetUrl) {
        enqueue(
            toE164,
            properties.getTemplates().getPasswordReset(),
            List.of(blankToDefault(recipientName, "parent"), blankToDefault(resetUrl, "")));
    }

    public void sendFeesPublished(
        String toE164,
        String parentName,
        String childNames,
        String className,
        String termName,
        String portalUrl) {
        enqueue(
            toE164,
            properties.getTemplates().getFees(),
            List.of(
                blankToDefault(parentName, "parent"),
                blankToDefault(termName, "this term"),
                blankToDefault(className, "their class"),
                blankToDefault(childNames, "your child"),
                blankToDefault(portalUrl, "")));
    }

    public void sendTermResultsPublished(
        String toE164,
        String parentName,
        String childNames,
        String termName,
        String portalUrl,
        List<WhatsAppDocument> documents) {
        enqueue(
            toE164,
            properties.getTemplates().getResults(),
            List.of(
                blankToDefault(parentName, "parent"),
                blankToDefault(termName, "this term"),
                blankToDefault(childNames, "your child"),
                blankToDefault(portalUrl, "")),
            documents);
    }

    public void sendExamTimetablePublished(
        String toE164,
        String parentName,
        String className,
        String portalUrl) {
        enqueue(
            toE164,
            properties.getTemplates().getExam(),
            List.of(
                blankToDefault(parentName, "parent"),
                blankToDefault(className, "class"),
                blankToDefault(portalUrl, "")));
    }

    private void enqueue(String toE164, String templateName, List<String> bodyParams) {
        enqueue(toE164, templateName, bodyParams, List.of());
    }

    private void enqueue(
        String toE164,
        String templateName,
        List<String> bodyParams,
        List<WhatsAppDocument> documents) {
        whatsAppPublisher.publish(WhatsAppJob.initial(toE164, templateName, bodyParams, documents));
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
