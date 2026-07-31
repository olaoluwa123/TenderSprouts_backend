package com.example.backend.security;

import com.example.backend.config.JwtProperties;
import com.example.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(SecurityUser user, long accessTokenVersion) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMs());
        return Jwts.builder()
            .subject(user.getUsername())
            .claim("userId", user.getUserId())
            .claim("role", user.getRole().name())
            .claim("profileId", user.getProfileId())
            .claim("atv", accessTokenVersion)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    public Long extractAccessTokenVersion(String token) {
        return parseToken(token).get("atv", Long.class);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractEmail(String token) {
        return parseToken(token).getSubject();
    }

    public User.Role extractRole(String token) {
        return User.Role.valueOf(parseToken(token).get("role", String.class));
    }
}
