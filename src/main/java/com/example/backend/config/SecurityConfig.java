package com.example.backend.config;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.RateLimitFilter;
import com.example.backend.security.RequireHttpsFilter;
import com.example.backend.security.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        SecurityHeadersFilter securityHeadersFilter,
        RequireHttpsFilter requireHttpsFilter,
        RateLimitFilter rateLimitFilter,
        JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/auth/change-password").authenticated()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/admission-enquiries").permitAll()
                .requestMatchers("/api/admission-enquiries", "/api/admission-enquiries/**").hasRole("ADMIN")
                .requestMatchers("/api/sessions/**").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/terms/**").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/students/**/grades").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/classes/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/subjects/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/grades/**").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/behavioural-reports/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/assessment-sheets/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/preschool-reports/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.GET, "/api/assessment-types").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/assessment-types/**").hasRole("ADMIN")
                .requestMatchers("/api/term-results/**").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/parents/me", "/api/parents/me/**").hasRole("PARENT")
                .requestMatchers(HttpMethod.GET, "/api/parents").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.GET, "/api/parents/{id:\\d+}").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/parents/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/enrollments/**").hasRole("ADMIN")
                .requestMatchers("/api/dashboard/teacher").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                .requestMatchers("/api/students/**/exam-timetable").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/students/**/report-cards/**").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/students/**/published-reports/**").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/classes/**/exam-timetable", "/api/classes/**/exam-timetable/**")
                    .hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.GET, "/api/calendar/**").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/calendar/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/announcements/inbox").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers(HttpMethod.GET, "/api/announcements/mine").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.POST, "/api/announcements/backfill-emails").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/announcements").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.PATCH, "/api/announcements/*").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.DELETE, "/api/announcements/*").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/announcements/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/attendance/pupil").hasAnyRole("ADMIN", "TEACHER", "PARENT")
                .requestMatchers("/api/attendance/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/fees/payments/webhook").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/fees/my").hasRole("PARENT")
                .requestMatchers(HttpMethod.POST, "/api/fees/payments/initialize").hasRole("PARENT")
                .requestMatchers(HttpMethod.POST, "/api/fees/payments/verify").hasRole("PARENT")
                .requestMatchers(HttpMethod.POST, "/api/fees/students/*/generate").hasRole("PARENT")
                .requestMatchers("/api/fees/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(requireHttpsFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
