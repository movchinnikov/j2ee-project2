package com.project.iac.web.dto.response;

import com.project.iac.domain.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public user representation — password hash is never included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile response")
public class UserResponse {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(example = "john_doe")
    private String username;

    @Schema(example = "john@example.com")
    private String email;

    @Schema(example = "John")
    private String firstName;

    @Schema(example = "Doe")
    private String lastName;

    @Schema(example = "true")
    private boolean enabled;

    @Schema(example = "[\"ROLE_USER\"]")
    private Set<String> roles;

    @Schema(example = "2024-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(example = "2024-01-15T09:30:00")
    private LocalDateTime updatedAt;

    /**
     * Maps a {@link UserEntity} to a {@link UserResponse}.
     * Excludes sensitive fields (passwordHash).
     */
    public static UserResponse from(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .enabled(entity.isEnabled())
                .roles(entity.getRoles().stream()
                        .map(r -> "ROLE_" + r.getName())
                        .collect(Collectors.toSet()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
