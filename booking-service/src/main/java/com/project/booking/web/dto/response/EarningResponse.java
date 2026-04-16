package com.project.booking.web.dto.response;

import com.project.booking.domain.entity.EarningEntity;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EarningResponse {
    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public static EarningResponse from(EarningEntity e) {
        return EarningResponse.builder()
                .id(e.getId()).orderId(e.getOrder().getId())
                .amount(e.getAmount()).createdAt(e.getCreatedAt()).build();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalEarned;
        private int completedOrders;
        private List<EarningResponse> history;
    }
}
