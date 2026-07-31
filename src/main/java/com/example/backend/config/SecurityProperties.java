package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private boolean requireHttps;
    private long hstsMaxAgeSeconds = 31_536_000L;
    private final RateLimit rateLimit = new RateLimit();

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    public long getHstsMaxAgeSeconds() {
        return hstsMaxAgeSeconds;
    }

    public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) {
        this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int authRequestsPerMinute = 10;
        private int apiRequestsPerMinute = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getAuthRequestsPerMinute() {
            return authRequestsPerMinute;
        }

        public void setAuthRequestsPerMinute(int authRequestsPerMinute) {
            this.authRequestsPerMinute = authRequestsPerMinute;
        }

        public int getApiRequestsPerMinute() {
            return apiRequestsPerMinute;
        }

        public void setApiRequestsPerMinute(int apiRequestsPerMinute) {
            this.apiRequestsPerMinute = apiRequestsPerMinute;
        }
    }
}
