package com.example.backend.security;

import com.example.backend.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequireHttpsFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    public RequireHttpsFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        if (!securityProperties.isRequireHttps()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (RequestSecuritySupport.isSecure(request, true)
            || RequestSecuritySupport.isLocalhost(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod())) {
            String redirectUrl = "https://" + request.getServerName();
            if (request.getServerPort() > 0 && request.getServerPort() != 443) {
                redirectUrl += ":" + request.getServerPort();
            }
            redirectUrl += request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) {
                redirectUrl += "?" + query;
            }
            response.setStatus(HttpStatus.PERMANENT_REDIRECT.value());
            response.setHeader("Location", redirectUrl);
            return;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("""
            {"status":403,"error":"Forbidden","message":"HTTPS is required"}
            """);
    }
}
