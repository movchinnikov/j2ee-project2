package com.project.order.web.dto.response;

import com.project.order.domain.entity.OrderEntity;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private UUID clientId;
    private UUID propertyId;
    private String propertyName;
    private String serviceTypeCode;
    private String serviceTypeName;
    private String frequency;
    private LocalDateTime scheduledDate;
    private BigDecimal durationHours;
    private BigDecimal basePrice;
    private BigDecimal discountPercent;
    private BigDecimal addonsPrice;
    private BigDecimal totalPrice;
    private String status;
    private String cleanerId;
    private String addons;
    private String notes;
    private LocalDateTime createdAt;

    /** null for ONE_TIME. For recurring: ID of the first order in the chain */
    private UUID parentOrderId;
    /** 0 = first/only, 1..N = subsequent occurrence */
    private int seriesIndex;
    /** Total sessions in series (1 = one-time) */
    private int seriesSize;

    public static OrderResponse from(OrderEntity o) {
        return OrderResponse.builder()
                .id(o.getId()).clientId(o.getClientId())
                .propertyId(o.getPropertyId()).propertyName(o.getPropertyName())
                .serviceTypeCode(o.getServiceTypeCode()).serviceTypeName(o.getServiceTypeName())
                .frequency(o.getFrequency()).scheduledDate(o.getScheduledDate())
                .durationHours(o.getDurationHours())
                .basePrice(o.getBasePrice()).discountPercent(o.getDiscountPercent())
                .addonsPrice(o.getAddonsPrice()).totalPrice(o.getTotalPrice())
                .status(o.getStatus())
                .cleanerId(o.getCleaner() != null ? o.getCleaner().getId().toString() : null)
                .addons(o.getAddonsSnapshot()).notes(o.getClientNotes())
                .createdAt(o.getCreatedAt())
                .parentOrderId(o.getParentOrderId())
                .seriesIndex(o.getSeriesIndex())
                .seriesSize(o.getSeriesSize())
                .build();
    }
}
