package com.project.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Kafka event emitted by IAC Service after a new user registers.
 * Topic: {@code user.registered}
 *
 * Consumed by:
 *   - order-service: auto-creates CleanerProfileEntity if role contains CLEANER
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    /** The newly created user's UUID. */
    private UUID userId;

    /** Username chosen at registration. */
    private String username;

    /** Email address registered. */
    private String email;

    /** First name. */
    private String firstName;

    /** Last name. */
    private String lastName;

    /** Initial set of role names assigned at registration (e.g., ["CLIENT"] or ["CLEANER"]). */
    private Set<String> roles;

    /** Timestamp when the user was created. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime registeredAt;
}
