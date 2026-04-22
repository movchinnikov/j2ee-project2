package com.project.property.web.controller;

import com.project.property.domain.entity.PropertyEntity;
import com.project.property.repository.PropertyRepository;
import com.project.property.web.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Properties", description = "Manage client cleaning properties")
public class PropertyController {

    private final PropertyRepository repo;

    @Data
    static class PropertyRequest {
        @NotBlank String name;
        @NotBlank String type;  // APARTMENT, HOUSE, OFFICE
        @NotNull @DecimalMin("10") BigDecimal areaSqm;
        @Min(1) @Max(10) int bathroomsCount = 1;
        String address;
        String notes;
    }

    @Data
    static class PropertyResponse {
        UUID id; String name; String type;
        BigDecimal areaSqm; int bathroomsCount;
        String address; String notes;

        static PropertyResponse from(PropertyEntity e) {
            var r = new PropertyResponse();
            r.id = e.getId(); r.name = e.getName(); r.type = e.getType();
            r.areaSqm = e.getAreaSqm(); r.bathroomsCount = e.getBathroomsCount();
            r.address = e.getAddress(); r.notes = e.getNotes();
            return r;
        }
    }

    @Operation(summary = "Add a property")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<ApiResponse<PropertyResponse>> add(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PropertyRequest req) {

        PropertyEntity e = PropertyEntity.builder()
                .clientId(clientId(user))
                .name(req.getName())
                .type(req.getType().toUpperCase())
                .areaSqm(req.getAreaSqm())
                .bathroomsCount(req.getBathroomsCount())
                .address(req.getAddress())
                .notes(req.getNotes())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Property added", PropertyResponse.from(repo.save(e))));
    }

    @Operation(summary = "List my properties")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PropertyResponse>>> list(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                repo.findAllByClientIdAndActiveTrue(clientId(user), PageRequest.of(page, size))
                        .map(PropertyResponse::from)));
    }

    @Operation(summary = "Get property by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PropertyResponse>> get(
            @AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {
        return repo.findByIdAsOption(id)
                .filter(p -> p.getClientId().equals(clientId(user)))
                .map(p -> ResponseEntity.ok(ApiResponse.<PropertyResponse>ok(PropertyResponse.from(p))))
                .getOrElse(() -> ResponseEntity.status(404).body(ApiResponse.error("Not found")));
    }

    @Operation(summary = "Update property")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<ApiResponse<PropertyResponse>> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody PropertyRequest req) {
        return repo.findByIdAsOption(id)
                .filter(p -> p.getClientId().equals(clientId(user)))
                .map(p -> {
                    p.setName(req.getName()); p.setType(req.getType().toUpperCase());
                    p.setAreaSqm(req.getAreaSqm()); p.setBathroomsCount(req.getBathroomsCount());
                    p.setAddress(req.getAddress()); p.setNotes(req.getNotes());
                    return ResponseEntity.ok(ApiResponse.<PropertyResponse>ok("Updated", PropertyResponse.from(repo.save(p))));
                })
                .getOrElse(() -> ResponseEntity.status(404).body(ApiResponse.error("Not found")));
    }

    @Operation(summary = "Delete (deactivate) property")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {
        return repo.findByIdAsOption(id)
                .filter(p -> p.getClientId().equals(clientId(user)))
                .map(p -> { p.setActive(false); repo.save(p);
                    return ResponseEntity.ok(ApiResponse.<Void>ok("Deleted", null)); })
                .getOrElse(() -> ResponseEntity.status(404).body(ApiResponse.error("Not found")));
    }

    // Internal endpoint — called by order-service to validate ownership
    @Operation(summary = "[Internal] Validate property ownership")
    @GetMapping("/{id}/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PropertyResponse>> validate(
            @PathVariable UUID id,
            @RequestParam UUID clientId) {
        return repo.findByIdAsOption(id)
                .filter(p -> p.getClientId().equals(clientId) && p.isActive())
                .map(p -> ResponseEntity.ok(ApiResponse.<PropertyResponse>ok(PropertyResponse.from(p))))
                .getOrElse(() -> ResponseEntity.status(404).body(ApiResponse.error("Property not found or access denied")));
    }

    private UUID clientId(UserDetails user) {
        return (user instanceof com.project.property.security.UserPrincipal p)
            ? p.getUserId()
            : UUID.nameUUIDFromBytes(user.getUsername().getBytes()); // fallback
    }
}
