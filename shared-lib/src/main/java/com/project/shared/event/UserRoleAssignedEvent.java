package com.project.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka event emitted by IAC Service after a role is assigned to a user.
 * Topic: {@code user.role_assigned}
 *
 * Consumed by:
 *   - order-service: if roleName == "CLEANER", auto-creates CleanerProfileEntity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleAssignedEvent {

    /** The user's UUID. */
    private UUID userId;

    /** The user's username. */
    private String username;

    /** The name of the role that was assigned (e.g., "CLEANER", "ADMIN"). */
    private String roleName;

    /** When the role was assigned. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime assignedAt;
}
