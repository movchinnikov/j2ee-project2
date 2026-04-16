package com.project.iac.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for user login.
 */
@Data
@Schema(description = "Login credentials")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username or email", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "Secret@123")
    private String password;
}
