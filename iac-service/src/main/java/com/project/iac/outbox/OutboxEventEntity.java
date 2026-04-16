package com.project.iac.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an outbox event record.
 *
 * Written atomically with the business transaction (same DB connection).
 * Polled by {@link OutboxRelayScheduler} and published to Kafka.
 *
 * Status lifecycle: PENDING → PROCESSED (or FAILED on error)
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Domain aggregate type — e.g. "User", "Order". */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /** UUID of the domain entity that generated this event. */
    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;

    /**
     * Kafka topic name — also used as the event type discriminator.
     * Example: "user.registered", "user.role_assigned"
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** JSON-serialised event payload (the shared-lib event DTO). */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Processing status.
     * PENDING   — not yet sent to Kafka
     * PROCESSED — successfully published
     * FAILED    — publishing failed (see errorMessage)
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
