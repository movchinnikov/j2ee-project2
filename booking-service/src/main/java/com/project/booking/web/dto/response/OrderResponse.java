package com.project.booking.web.dto.response;

import com.project.booking.domain.entity.OrderEntity;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private UUID clientId;
    private UUID propertyId;
    private String propertyName;
    private String serviceType;
    private String frequency;
    private LocalDateTime scheduledDate;
    private BigDecimal durationHours;
    private BigDecimal basePrice;
    private BigDecimal discountPercent;
    private BigDecimal addonsPrice;
    private BigDecimal totalPrice;
    private String status;
    private String cleanerAssigned;
    private Set<String> addons;
    private String notes;
    private LocalDateTime createdAt;

    public static OrderResponse from(OrderEntity o) {
        return OrderResponse.builder()
                .id(o.getId())
                .clientId(o.getClientId())
                .propertyId(o.getProperty().getId())
                .propertyName(o.getProperty().getName())
                .serviceType(o.getServiceType().getName())
                .frequency(o.getFrequency().name())
                .scheduledDate(o.getScheduledDate())
                .durationHours(o.getDurationHours())
                .basePrice(o.getBasePrice())
                .discountPercent(o.getDiscountPercent())
                .addonsPrice(o.getAddonsPrice())
                .totalPrice(o.getTotalPrice())
                .status(o.getStatus().name())
                .cleanerAssigned(o.getCleaner() != null ? o.getCleaner().getId().toString() : null)
                .addons(o.getAddons().stream().map(a -> a.getName()).collect(Collectors.toSet()))
                .notes(o.getClientNotes())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
