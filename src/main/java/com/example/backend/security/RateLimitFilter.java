package com.example.backend.security;

import com.example.backend.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String clientIp = ClientIpResolver.resolve(request);
        boolean allowed = isAuthPath(request.getRequestURI())
            ? rateLimitService.tryConsumeAuth(clientIp)
            : rateLimitService.tryConsumeApi(clientIp);

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(
                response.getWriter(),
                Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Rate limit exceeded. Please try again later."));
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/actuator/");
    }

    private boolean isAuthPath(String uri) {
        return uri != null && uri.startsWith("/api/auth/");
    }
}
