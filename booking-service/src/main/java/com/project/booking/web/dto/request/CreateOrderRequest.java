package com.project.booking.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Schema(description = "Create a cleaning order")
public class CreateOrderRequest {

    @NotNull
    @Schema(description = "Property ID to clean")
    private UUID propertyId;

    @NotNull
    @Schema(description = "Service type ID")
    private UUID serviceTypeId;

    @Schema(description = "Additional service IDs")
    private Set<UUID> addonIds;

    @Schema(example = "ONE_TIME", allowableValues = {"ONE_TIME","WEEKLY","BIWEEKLY","MONTHLY"})
    private String frequency = "ONE_TIME";

    @NotNull @FutureOrPresent
    @Schema(description = "When to clean", example = "2025-05-01T10:00:00")
    private LocalDateTime scheduledDate;

    @Schema(example = "Please use eco-friendly products")
    private String notes;
}
