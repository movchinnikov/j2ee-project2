package com.project.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Set;

/**
 * HTTP client for pricing-service.
 * Calls POST /api/v1/pricing/calculate to get price breakdown.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PricingClient {

    @Value("${services.pricing-url}")
    private String pricingUrl;

    @Data
    public static class PriceRequest {
        private BigDecimal areaSqm;
        private int bathroomsCount;
        private String serviceTypeCode;
        private Set<String> addonCodes;
        private String frequency;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceResult {
        private BigDecimal basePrice;
        private BigDecimal discountPercent;
        private BigDecimal addonsPrice;
        private BigDecimal totalPrice;
        private BigDecimal durationHours;
        private String serviceTypeCode;
        private String serviceTypeName;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class ApiWrapper { private boolean success; private PriceResult data; }

    public PriceResult calculatePrice(PriceRequest req) {
        try {
            ApiWrapper resp = RestClient.create(pricingUrl)
                    .post().uri("/api/v1/pricing/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(ApiWrapper.class);
            return resp != null && resp.isSuccess() ? resp.getData() : null;
        } catch (Exception e) {
            log.error("Pricing service call failed: {}", e.getMessage());
            return null;
        }
    }
}
