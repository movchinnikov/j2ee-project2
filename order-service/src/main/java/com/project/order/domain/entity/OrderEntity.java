package com.project.order.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "orders", schema = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = "id") @ToString
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "client_id", nullable = false) private UUID clientId;
    @Column(name = "property_id", nullable = false) private UUID propertyId;
    @Column(name = "property_name", length = 100) private String propertyName;
    @Column(name = "service_type_code", length = 50) private String serviceTypeCode;
    @Column(name = "service_type_name", length = 100) private String serviceTypeName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleaner_id") private CleanerProfileEntity cleaner;
    @Column(nullable = false, length = 20) @Builder.Default private String frequency = "ONE_TIME";
    @Column(name = "scheduled_date", nullable = false) private LocalDateTime scheduledDate;
    @Column(name = "duration_hours", precision = 5, scale = 2) private BigDecimal durationHours;
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal basePrice = BigDecimal.ZERO;
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2) @Builder.Default private BigDecimal discountPercent = BigDecimal.ZERO;
    @Column(name = "addons_price", nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal addonsPrice = BigDecimal.ZERO;
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal totalPrice = BigDecimal.ZERO;
    @Column(nullable = false, length = 30) @Builder.Default private String status = "PENDING";
    @Column(name = "cancellation_reason", columnDefinition = "TEXT") private String cancellationReason;
    @Column(name = "client_notes", columnDefinition = "TEXT") private String clientNotes;
    @Column(name = "addons_snapshot", columnDefinition = "TEXT") private String addonsSnapshot;

    /** null for standalone orders; set to first order's ID for recurring siblings */
    @Column(name = "parent_order_id") private UUID parentOrderId;
    /** 0 = first/only, 1..N for subsequent occurrences */
    @Column(name = "series_index", nullable = false) @Builder.Default private int seriesIndex = 0;
    /** Total number of orders in this series (1 = one-time) */
    @Column(name = "series_size", nullable = false) @Builder.Default private int seriesSize = 1;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
