package com.loglife.nutrition.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Guards {@code /api/**} with a static token in the {@code X-Api-Token} header. The PWA shell and
 * actuator stay open (the shell contains no data; the API does). Comparison is constant-time.
 * Registered only when a token is configured — see {@link WebSecurityConfig}.
 */
public class ApiTokenFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Api-Token";

    private final byte[] expected;

    public ApiTokenFilter(String token) {
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided != null
                && MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        // Never log the provided value — it may be a mistyped real secret.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\","
                + "\"message\":\"Missing or invalid X-Api-Token\"}");
    }
}
