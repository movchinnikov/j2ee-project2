package com.project.booking.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Create or update a property")
public class PropertyRequest {

    @NotBlank
    @Schema(example = "My Apartment", description = "Property name")
    private String name;

    @NotBlank
    @Schema(example = "APARTMENT", allowableValues = {"APARTMENT","HOUSE","OFFICE"})
    private String type;

    @NotNull @DecimalMin("10")
    @Schema(example = "65.5", description = "Total area in sqm")
    private BigDecimal areaSqm;

    @Min(1) @Max(10)
    @Schema(example = "2", description = "Number of bathrooms")
    private int bathroomsCount = 1;

    @Schema(example = "123 Main St, Toronto, ON")
    private String address;

    @Schema(example = "Has a dog, please bring pet-safe products")
    private String notes;
}
