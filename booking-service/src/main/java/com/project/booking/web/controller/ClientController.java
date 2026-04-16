package com.project.booking.web.controller;

import com.project.booking.domain.entity.PropertyEntity;
import com.project.booking.domain.enums.PropertyType;
import com.project.booking.service.OrderService;
import com.project.booking.service.UnitOfWork;
import com.project.booking.web.dto.request.CreateOrderRequest;
import com.project.booking.web.dto.request.PropertyRequest;
import com.project.booking.web.dto.response.ApiResponse;
import com.project.booking.web.dto.response.OrderResponse;
import com.project.booking.web.dto.response.PropertyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CLIENT')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Client", description = "Property and order management for clients")
public class ClientController {

    private final UnitOfWork uow;
    private final OrderService orderService;

    // ── Properties ────────────────────────────────────────────────────────

    @Operation(summary = "Add a property")
    @PostMapping("/properties")
    public ResponseEntity<ApiResponse<PropertyResponse>> addProperty(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PropertyRequest req) {

        UUID clientId = resolveClientId(user);
        PropertyEntity property = PropertyEntity.builder()
                .clientId(clientId)
                .name(req.getName())
                .type(PropertyType.valueOf(req.getType().toUpperCase()))
                .areaSqm(req.getAreaSqm())
                .bathroomsCount(req.getBathroomsCount())
                .address(req.getAddress())
                .notes(req.getNotes())
                .build();
        PropertyEntity saved = uow.getProperties().save(property);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Property added", PropertyResponse.from(saved)));
    }

    @Operation(summary = "List my properties")
    @GetMapping("/properties")
    public ResponseEntity<ApiResponse<Page<PropertyResponse>>> myProperties(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID clientId = resolveClientId(user);
        Page<PropertyResponse> props = uow.getProperties()
                .findAllByClientIdAndActiveTrue(clientId, PageRequest.of(page, size))
                .map(PropertyResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(props));
    }

    @Operation(summary = "Get property by ID")
    @GetMapping("/properties/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getProperty(
            @AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {
        return uow.getProperties().findByIdAsOption(id)
                .filter(p -> p.getClientId().equals(resolveClientId(user)))
                .map(p -> ResponseEntity.ok(ApiResponse.<PropertyResponse>ok(PropertyResponse.from(p))))
                .getOrElse(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Property not found")));
    }

    // ── Orders ────────────────────────────────────────────────────────────

    @Operation(summary = "Create a cleaning order (price is calculated automatically)")
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateOrderRequest req) {

        return orderService.createOrder(resolveClientId(user), req)
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.<OrderResponse>error("Order failed", err)),
                        order -> ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok("Order created", order))
                );
    }

    @Operation(summary = "My orders")
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> myOrders(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                orderService.getClientOrders(resolveClientId(user), PageRequest.of(page, size))));
    }

    @Operation(summary = "Get order details")
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {

        return orderService.getOrderById(id, resolveClientId(user))
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        o -> ResponseEntity.ok(ApiResponse.ok(o))
                );
    }

    @Operation(summary = "Cancel order")
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {

        return orderService.cancelOrder(id, resolveClientId(user), reason)
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                        o -> ResponseEntity.ok(ApiResponse.ok("Order cancelled", o))
                );
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * In a real system we'd look up userId from a user registry.
     * Here we use the username as a UUID seed for demo purposes.
     * BookingService stores client_id = the UUID from IAC user response.
     */
    private UUID resolveClientId(UserDetails user) {
        return UUID.nameUUIDFromBytes(user.getUsername().getBytes());
    }
}
