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
 * Kafka event emitted by Order Service when an order is paid and a cleaner is assigned (status=CONFIRMED).
 * Topic: {@code order.confirmed}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {

    private UUID orderId;
    private UUID clientId;

    /** UUID of the assigned cleaner's profile — null if no cleaners available. */
    private UUID cleanerProfileId;

    /** User ID of the assigned cleaner — null if no cleaners available. */
    private UUID cleanerUserId;

    private BigDecimal totalPrice;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime confirmedAt;
}
