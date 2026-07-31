package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private int maxFailedAttempts = 5;
    private int maxFailedAttemptsPerIp = 25;
    private int lockoutMinutes = 15;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public int getMaxFailedAttemptsPerIp() {
        return maxFailedAttemptsPerIp;
    }

    public void setMaxFailedAttemptsPerIp(int maxFailedAttemptsPerIp) {
        this.maxFailedAttemptsPerIp = maxFailedAttemptsPerIp;
    }

    public int getLockoutMinutes() {
        return lockoutMinutes;
    }

    public void setLockoutMinutes(int lockoutMinutes) {
        this.lockoutMinutes = lockoutMinutes;
    }
}
