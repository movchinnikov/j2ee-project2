package com.project.property.security;

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
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

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
                    var authorities = jwtValidator.extractRoles(token).stream()
                        .map(SimpleGrantedAuthority::new).toList();

                    // Wrap in UserDetails so @AuthenticationPrincipal UserDetails works in controllers
                    var userDetails = User.withUsername(jwtValidator.extractUsername(token))
                        .password("")
                        .authorities(authorities)
                        .build();

                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities));
                } catch (Exception e) { log.warn("JWT filter error: {}", e.getMessage()); }
            }
        }
        chain.doFilter(req, res);
    }
}
