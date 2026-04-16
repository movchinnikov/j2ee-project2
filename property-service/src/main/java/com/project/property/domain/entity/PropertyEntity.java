package com.project.property.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "properties", schema = "property")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = "id") @ToString
public class PropertyEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String type;  // APARTMENT, HOUSE, OFFICE

    @Column(name = "area_sqm", nullable = false, precision = 8, scale = 2)
    private BigDecimal areaSqm;

    @Column(name = "bathrooms_count", nullable = false)
    @Builder.Default private int bathroomsCount = 1;

    @Column(columnDefinition = "TEXT") private String address;
    @Column(columnDefinition = "TEXT") private String notes;

    @Column(nullable = false) @Builder.Default private boolean active = true;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
