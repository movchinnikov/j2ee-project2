package com.project.iac.service;

import com.project.iac.domain.entity.RefreshTokenEntity;
import com.project.iac.domain.entity.RoleEntity;
import com.project.iac.domain.entity.UserEntity;
import com.project.iac.outbox.OutboxPublisher;
import com.project.iac.security.JwtTokenProvider;
import com.project.iac.security.UserDetailsServiceImpl;
import com.project.iac.web.dto.request.LoginRequest;
import com.project.iac.web.dto.request.RegisterRequest;
import com.project.iac.web.dto.response.AuthResponse;
import com.project.iac.web.dto.response.UserResponse;
import com.project.shared.event.UserRegisteredEvent;
import com.project.shared.kafka.KafkaTopics;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication service — handles register, login, refresh, and logout.
 * Uses Vavr {@link Either} for functional error propagation without exceptions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UnitOfWork uow;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final OutboxPublisher outboxPublisher;

    @Value("${jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    // ── Register ────────────────────────────────────────────────────────────

    /**
     * Register a new user with the role specified in the request (CLIENT or CLEANER).
     *
     * @return Either.left(errorMessage) or Either.right(AuthResponse)
     */
    @Transactional
    public Either<String, AuthResponse> register(RegisterRequest req) {
        if (uow.getUsers().existsByUsername(req.getUsername())) {
            return Either.left("Username '" + req.getUsername() + "' is already taken");
        }
        if (uow.getUsers().existsByEmail(req.getEmail())) {
            return Either.left("Email '" + req.getEmail() + "' is already registered");
        }

        // Determine platform role: CLIENT or CLEANER (default to CLIENT)
        String roleName = (req.getRole() != null &&
                (req.getRole().equalsIgnoreCase("CLEANER") || req.getRole().equalsIgnoreCase("CLIENT")))
                ? req.getRole().toUpperCase()
                : "CLIENT";

        RoleEntity platformRole = uow.getRoles().findByNameAsOption(roleName)
                .getOrElseThrow(() -> new IllegalStateException(roleName + " role not found — check Flyway migration"));

        UserEntity user = UserEntity.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .enabled(true)
                .build();
        user.addRole(platformRole);

        UserEntity saved = uow.getUsers().save(user);
        log.info("Registered new {} user: {}", roleName, saved.getUsername());

        // ── Outbox: publish UserRegisteredEvent ──────────────────────────────
        Set<String> roleNames = saved.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
        outboxPublisher.publish(
                "User",
                saved.getId().toString(),
                KafkaTopics.USER_REGISTERED,
                UserRegisteredEvent.builder()
                        .userId(saved.getId())
                        .username(saved.getUsername())
                        .email(saved.getEmail())
                        .firstName(saved.getFirstName())
                        .lastName(saved.getLastName())
                        .roles(roleNames)
                        .registeredAt(LocalDateTime.now())
                        .build()
        );

        return Either.right(buildAuthResponse(saved));
    }


    // ── Login ──────────────────────────────────────────────────────────────

    /**
     * Authenticate user credentials and issue JWT tokens.
     */
    @Transactional
    public Either<String, AuthResponse> login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (BadCredentialsException e) {
            return Either.left("Invalid username or password");
        }

        UserEntity user = uow.getUsers().findByUsernameWithRoles(req.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        if (!user.isEnabled()) {
            return Either.left("User account is disabled");
        }

        log.info("User logged in: {}", user.getUsername());
        return Either.right(buildAuthResponse(user));
    }

    // ── Refresh ────────────────────────────────────────────────────────────

    /**
     * Issue a new access token using a valid refresh token.
     */
    @Transactional
    public Either<String, AuthResponse> refresh(String refreshToken) {
        return uow.getRefreshTokens().findByTokenAsOption(refreshToken)
                .toEither("Refresh token not found")
                .flatMap(rt -> {
                    if (!rt.isValid()) {
                        return Either.left("Refresh token is expired or revoked");
                    }

                    UserEntity user = rt.getUser();
                    // Rotate refresh token
                    rt.setRevoked(true);
                    uow.getRefreshTokens().save(rt);

                    return Either.right(buildAuthResponse(user));
                });
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    /**
     * Revoke all refresh tokens for the given user.
     */
    @Transactional
    public void logout(String username) {
        uow.getUsers().findByUsernameAsOption(username).forEach(user -> {
            uow.getRefreshTokens().revokeAllByUserId(user.getId());
            log.info("Logged out user: {}. All refresh tokens revoked.", username);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(UserEntity user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        // Pass real DB UUID so the 'uid' JWT claim has the authoritative IAC user ID
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails, user.getId());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiryMs / 1000)
                .user(UserResponse.from(user))
                .build();
    }

    private String createRefreshToken(UserEntity user) {
        RefreshTokenEntity rt = RefreshTokenEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiryMs / 1000))
                .revoked(false)
                .build();
        return uow.getRefreshTokens().save(rt).getToken();
    }
}
