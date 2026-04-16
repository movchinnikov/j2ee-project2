package com.project.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka event emitted by Order Service when a cleaner marks the order complete (status=COMPLETED).
 * Topic: {@code order.completed}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {

    private UUID orderId;
    private UUID clientId;
    private UUID cleanerProfileId;
    private UUID cleanerUserId;
    private BigDecimal totalPrice;

    /** Cleaner's earning for this order (60% of totalPrice). */
    private BigDecimal cleanerEarning;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
}
