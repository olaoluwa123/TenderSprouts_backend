package com.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestSecuritySupport {

    private RequestSecuritySupport() {
    }

    public static boolean isSecure(HttpServletRequest request, boolean trustProxyHeaders) {
        if (request.isSecure()) {
            return true;
        }
        if (!trustProxyHeaders) {
            return false;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.equalsIgnoreCase("https");
    }

    /** Allows plain HTTP only when the request Host is local (typical dev setup). */
    public static boolean isLocalhost(HttpServletRequest request) {
        String host = request.getServerName();
        return "localhost".equalsIgnoreCase(host)
            || "127.0.0.1".equals(host)
            || "::1".equals(host);
    }
}
