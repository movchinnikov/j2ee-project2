package com.project.pricing.web.controller;

import com.project.pricing.domain.entity.ServiceAddonEntity;
import com.project.pricing.domain.entity.ServiceTypeEntity;
import com.project.pricing.repository.ServiceAddonRepository;
import com.project.pricing.repository.ServiceTypeRepository;
import com.project.pricing.service.PricingEngine;
import com.project.pricing.web.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Pricing", description = "Service catalog and price calculation")
public class PricingController {

    private final ServiceTypeRepository serviceTypeRepo;
    private final ServiceAddonRepository serviceAddonRepo;
    private final PricingEngine engine;

    // ── Catalog (public) ─────────────────────────────────────────────────

    @Operation(summary = "List all service types with rates")
    @GetMapping("/catalog/services")
    public ResponseEntity<ApiResponse<List<ServiceTypeEntity>>> getServices() {
        return ResponseEntity.ok(ApiResponse.ok(serviceTypeRepo.findAllByActiveTrue()));
    }

    @Operation(summary = "List all service addons with prices")
    @GetMapping("/catalog/addons")
    public ResponseEntity<ApiResponse<List<ServiceAddonEntity>>> getAddons() {
        return ResponseEntity.ok(ApiResponse.ok(serviceAddonRepo.findAllByActiveTrue()));
    }

    // ── Price calculation ─────────────────────────────────────────────────

    @Data
    static class PriceRequest {
        @NotNull @DecimalMin("10") BigDecimal areaSqm;
        @Min(1) @Max(20) int bathroomsCount = 1;
        @NotBlank String serviceTypeCode;
        Set<String> addonCodes;
        String frequency = "ONE_TIME";
    }

    @Operation(summary = "Calculate cleaning order price",
               description = "Pass areaSqm, serviceTypeCode, optional addonCodes, and frequency. Returns full price breakdown.")
    @PostMapping("/pricing/calculate")
    public ResponseEntity<ApiResponse<PricingEngine.PricingResult>> calculate(
            @Valid @RequestBody PriceRequest req) {

        return serviceTypeRepo.findByCode(req.getServiceTypeCode().toUpperCase())
                .toEither("Service type '" + req.getServiceTypeCode() + "' not found")
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                        serviceType -> {
                            Set<ServiceAddonEntity> addons = req.getAddonCodes() != null && !req.getAddonCodes().isEmpty()
                                    ? serviceAddonRepo.findAllByCodeIn(req.getAddonCodes())
                                    : Set.of();

                            String freq = req.getFrequency() != null ? req.getFrequency() : "ONE_TIME";
                            var result = engine.calculate(req.getAreaSqm(), req.getBathroomsCount(),
                                    serviceType, addons, freq);
                            return ResponseEntity.ok(ApiResponse.ok("Price calculated", result));
                        }
                );
    }
}
