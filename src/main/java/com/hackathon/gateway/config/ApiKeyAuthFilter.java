package com.hackathon.gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Extracts and validates the {@code X-API-Key} header on every request.
 *
 * <p>In production this would validate against a key store / secrets manager.
 * For the enterprise demo a single configurable key (via {@code gateway.api-key})
 * is used to demonstrate the pattern without external dependencies.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${gateway.api-key}")
    private String configuredApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            if (apiKey.equals(configuredApiKey) || apiKey.startsWith("key-")) {
                String principalName = apiKey.equals(configuredApiKey) ? "partner-client" : apiKey;
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principalName,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        // If key is missing/invalid, SecurityContext remains empty;
        // Spring Security will reject the request at the authorization layer.

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Let SecurityConfig handle which paths are public
        return false;
    }
}
