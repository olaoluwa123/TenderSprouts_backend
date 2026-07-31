package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        CustomUserDetailsService userDetailsService,
        UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String email = jwtService.extractEmail(token);
                Long tokenVersion = jwtService.extractAccessTokenVersion(token);
                User userEntity = userRepository.findByEmail(email).orElse(null);
                if (userEntity == null
                    || tokenVersion == null
                    || !tokenVersion.equals(userEntity.getAccessTokenVersion())) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityUser user = (SecurityUser) userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
