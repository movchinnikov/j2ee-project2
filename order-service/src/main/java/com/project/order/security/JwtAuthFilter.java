package com.project.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component @RequiredArgsConstructor @Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenValidator jwtValidator;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtValidator.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    List<SimpleGrantedAuthority> authorities = jwtValidator.extractRoles(token).stream()
                        .map(SimpleGrantedAuthority::new).toList();

                    String username = jwtValidator.extractUsername(token);
                    UUID userId = jwtValidator.extractUserId(token);

                    // Use UserPrincipal so controllers can access the real IAC userId
                    UserPrincipal principal = new UserPrincipal(username, authorities, userId);

                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));

                    log.debug("Authenticated user='{}' uid={}", username, userId);
                } catch (Exception e) { log.warn("JWT filter error: {}", e.getMessage()); }
            }
        }
        chain.doFilter(req, res);
    }
}
