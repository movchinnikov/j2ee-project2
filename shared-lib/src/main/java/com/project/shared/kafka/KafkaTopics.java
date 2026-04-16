package com.project.shared.kafka;

/**
 * Kafka topic name constants shared across all Cleaning Platform services.
 *
 * Naming convention:  {domain}.{event}
 * Partitions:         3 (default) — allows parallel consumption
 * Retention:          7 days (default broker setting)
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    // ── User / IAC events ─────────────────────────────────────────────────
    /** Published by IAC Service after successful user registration. */
    public static final String USER_REGISTERED    = "user.registered";

    /** Published by IAC Service after a role is assigned to a user. */
    public static final String USER_ROLE_ASSIGNED = "user.role_assigned";

    /** Published by IAC Service after a role is revoked from a user. */
    public static final String USER_ROLE_REVOKED  = "user.role_revoked";

    // ── Order events ──────────────────────────────────────────────────────
    /** Published by Order Service when a new order is created (status=PENDING). */
    public static final String ORDER_CREATED   = "order.created";

    /** Published by Order Service when an order is paid and a cleaner is assigned (status=CONFIRMED). */
    public static final String ORDER_CONFIRMED = "order.confirmed";

    /** Published by Order Service when a cleaner marks the order complete (status=COMPLETED). */
    public static final String ORDER_COMPLETED = "order.completed";

    /** Published by Order Service when a client cancels an order (status=CANCELLED). */
    public static final String ORDER_CANCELLED = "order.cancelled";
}
