package com.project.property.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Custom UserDetails implementation that carries the real IAC user UUID
 * extracted from the JWT 'uid' claim.
 * This eliminates the UUID.nameUUIDFromBytes(username) anti-pattern.
 */
@Getter
public class UserPrincipal extends User {

    /** The authoritative user UUID from IAC service (embedded in JWT as 'uid' claim) */
    private final UUID userId;

    public UserPrincipal(String username, Collection<? extends GrantedAuthority> authorities, UUID userId) {
        super(username, "", authorities);
        this.userId = userId;
    }
}
