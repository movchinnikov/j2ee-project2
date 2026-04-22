package com.project.property.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component @Slf4j
public class JwtTokenValidator {
    @Value("${jwt.secret}") private String secret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(
            Base64.getEncoder().encodeToString(secret.getBytes())));
    }

    public boolean isValid(String token) {
        try { Jwts.parser().verifyWith(key()).build().parseSignedClaims(token); return true; }
        catch (Exception e) { log.debug("JWT invalid: {}", e.getMessage()); return false; }
    }

    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload().getSubject();
    }

    /**
     * Extract the real IAC user UUID from the 'uid' claim.
     * Falls back to nameUUIDFromBytes(username) for backward compatibility
     * with tokens issued before the 'uid' claim was added.
     */
    public UUID extractUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload();
        String uid = (String) claims.get("uid");
        if (uid != null) {
            try { return UUID.fromString(uid); } catch (IllegalArgumentException e) { /* fall through */ }
        }
        // Fallback: derive from username (matches old tokens, may not match DB)
        String username = claims.getSubject();
        log.warn("JWT missing 'uid' claim for user '{}' — using nameUUIDFromBytes fallback", username);
        return UUID.nameUUIDFromBytes(username.getBytes());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object r = Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload().get("roles");
        return r instanceof List<?> l ? l.stream().map(Object::toString).toList() : List.of();
    }
}
