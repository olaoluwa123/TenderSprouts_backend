package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.whatsapp")
public class AppWhatsAppProperties {

    private boolean enabled;
    private String apiVersion = "v21.0";
    private String phoneNumberId = "";
    private String accessToken = "";
    private String languageCode = "en";
    private final Templates templates = new Templates();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Templates getTemplates() {
        return templates;
    }

    public static class Templates {
        private String welcome = "portal_welcome";
        private String passwordReset = "portal_password_reset";
        private String fees = "fees_published";
        private String results = "results_published";
        private String exam = "exam_timetable_published";

        public String getWelcome() {
            return welcome;
        }

        public void setWelcome(String welcome) {
            this.welcome = welcome;
        }

        public String getPasswordReset() {
            return passwordReset;
        }

        public void setPasswordReset(String passwordReset) {
            this.passwordReset = passwordReset;
        }

        public String getFees() {
            return fees;
        }

        public void setFees(String fees) {
            this.fees = fees;
        }

        public String getResults() {
            return results;
        }

        public void setResults(String results) {
            this.results = results;
        }

        public String getExam() {
            return exam;
        }

        public void setExam(String exam) {
            this.exam = exam;
        }
    }
}
