package com.project.iac.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth response — returned after login and token refresh.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication token response")
public class AuthResponse {

    @Schema(description = "JWT access token (valid 15 min)", example = "eyJhbGci...")
    private String accessToken;

    @Schema(description = "JWT refresh token (valid 7 days)", example = "550e8400-e29b...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token TTL in seconds", example = "900")
    private long expiresIn;

    @Schema(description = "Authenticated user info")
    private UserResponse user;
}
