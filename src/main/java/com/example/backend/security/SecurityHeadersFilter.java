package com.example.backend.security;

import com.example.backend.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    public SecurityHeadersFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        if (RequestSecuritySupport.isSecure(request, true)) {
            response.setHeader(
                "Strict-Transport-Security",
                "max-age=" + securityProperties.getHstsMaxAgeSeconds() + "; includeSubDomains");
        }
        filterChain.doFilter(request, response);
    }
}
