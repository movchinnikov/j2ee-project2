package com.project.iac.web.controller;

import com.project.iac.service.AuthService;
import com.project.iac.web.dto.request.LoginRequest;
import com.project.iac.web.dto.request.RegisterRequest;
import com.project.iac.web.dto.response.ApiResponse;
import com.project.iac.web.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 * Handles registration, login, token refresh, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh tokens, logout")
public class AuthController {

    private final AuthService authService;

    // ── POST /register ──────────────────────────────────────────────────────

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the USER role. Returns access and refresh tokens."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or username/email already taken",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        return authService.register(request)
                .fold(
                        error -> ResponseEntity
                                .badRequest()
                                .body(ApiResponse.error("Registration failed", error)),
                        auth -> ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.ok("User registered successfully", auth))
                );
    }

    // ── POST /login ─────────────────────────────────────────────────────────

    @Operation(
            summary = "Login",
            description = "Authenticate with username and password. Returns JWT access and refresh tokens."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request)
                .fold(
                        error -> ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error("Authentication failed", error)),
                        auth -> ResponseEntity.ok(ApiResponse.ok("Login successful", auth))
                );
    }

    // ── POST /refresh ───────────────────────────────────────────────────────

    @Operation(
            summary = "Refresh access token",
            description = "Use a valid refresh token to get a new access token. The old refresh token is rotated."
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestParam String refreshToken) {

        return authService.refresh(refreshToken)
                .fold(
                        error -> ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error("Token refresh failed", error)),
                        auth -> ResponseEntity.ok(ApiResponse.ok("Token refreshed", auth))
                );
    }

    // ── POST /logout ────────────────────────────────────────────────────────

    @Operation(
            summary = "Logout",
            description = "Revokes all refresh tokens for the authenticated user.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}
