package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationMs = 900_000;
    private long refreshTokenExpirationMs = 604_800_000;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }
}
