package com.project.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HTTP client for property-service.
 * Calls GET /api/v1/properties/{id}/validate to verify that client owns the property.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PropertyClient {

    @Value("${services.property-url}")
    private String propertyUrl;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertyInfo {
        private UUID id;
        private String name;
        private String type;
        private BigDecimal areaSqm;
        private int bathroomsCount;
        private String address;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class ApiWrapper { private boolean success; private PropertyInfo data; }

    /**
     * Returns PropertyInfo if the property exists and belongs to the clientId.
     * Returns null if not found / not owned.
     */
    public PropertyInfo validateProperty(UUID propertyId, UUID clientId, String bearerToken) {
        try {
            ApiWrapper resp = RestClient.create(propertyUrl)
                    .get()
                    .uri("/api/v1/properties/{id}/validate?clientId={cid}", propertyId, clientId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(ApiWrapper.class);
            return resp != null && resp.isSuccess() ? resp.getData() : null;
        } catch (Exception e) {
            log.error("Property service call failed for {}: {}", propertyId, e.getMessage());
            return null;
        }
    }
}
