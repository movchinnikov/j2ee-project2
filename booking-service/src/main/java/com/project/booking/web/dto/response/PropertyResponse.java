package com.project.booking.web.dto.response;

import com.project.booking.domain.entity.PropertyEntity;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyResponse {
    private UUID id;
    private String name;
    private String type;
    private BigDecimal areaSqm;
    private int bathroomsCount;
    private String address;
    private String notes;
    private LocalDateTime createdAt;

    public static PropertyResponse from(PropertyEntity e) {
        return PropertyResponse.builder()
                .id(e.getId()).name(e.getName()).type(e.getType().name())
                .areaSqm(e.getAreaSqm()).bathroomsCount(e.getBathroomsCount())
                .address(e.getAddress()).notes(e.getNotes()).createdAt(e.getCreatedAt())
                .build();
    }
}
