package com.project.iac.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload for user registration.
 * Role field determines platform role: CLIENT or CLEANER.
 */
@Data
@Schema(description = "User registration request")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username can only contain letters, digits, _, -, .")
    @Schema(description = "Unique username", example = "john_doe")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Password (min 6 chars)", example = "secret123")
    private String password;

    @Size(max = 100)
    @Schema(description = "First name", example = "John")
    private String firstName;

    @Size(max = 100)
    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    /**
     * Platform role chosen at registration.
     * Accepted values: CLIENT, CLEANER
     * Defaults to CLIENT if not specified.
     */
    @Schema(description = "Platform role", example = "CLIENT", allowableValues = {"CLIENT", "CLEANER"})
    private String role = "CLIENT";
}
