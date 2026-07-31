package com.example.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.backend.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequireHttpsFilterTest {

    @Test
    void blocksInsecureRequestsWhenHttpsRequired() throws Exception {
        SecurityProperties properties = new SecurityProperties();
        properties.setRequireHttps(true);
        RequireHttpsFilter filter = new RequireHttpsFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServerName("api.school.local");
        request.setSecure(false);
        request.addHeader("X-Forwarded-Proto", "http");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, org.mockito.Mockito.never()).doFilter(request, response);
    }

    @Test
    void allowsLocalhostDuringDevelopment() throws Exception {
        SecurityProperties properties = new SecurityProperties();
        properties.setRequireHttps(true);
        RequireHttpsFilter filter = new RequireHttpsFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServerName("localhost");
        request.setSecure(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
