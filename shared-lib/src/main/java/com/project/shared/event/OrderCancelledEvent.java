package com.project.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka event emitted by Order Service when a client cancels an order (status=CANCELLED).
 * Topic: {@code order.cancelled}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {

    private UUID orderId;
    private UUID clientId;

    /** UUID of the cleaner who was assigned (null if none was assigned yet). */
    private UUID cleanerProfileId;

    /** Reason for cancellation as provided by the client. */
    private String cancellationReason;

    /** The status the order was in when cancelled (PENDING or CONFIRMED). */
    private String previousStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledAt;
}
