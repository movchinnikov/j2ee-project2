package com.project.order.web.controller;

import com.project.order.domain.entity.OrderEntity;
import com.project.order.service.OrderService;
import com.project.order.web.dto.response.ApiResponse;
import com.project.order.web.dto.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Client Orders", description = "Create and manage cleaning orders")
public class ClientOrderController {

    private final OrderService orderService;

    @Data
    static class CreateOrderRequest {
        @NotNull UUID propertyId;
        @NotBlank String serviceTypeCode;
        Set<String> addonCodes;
        String frequency = "ONE_TIME";
        @NotNull @FutureOrPresent LocalDateTime scheduledDate;
        String notes;
    }

    @Operation(summary = "Create a cleaning order",
               description = "Creates order in PENDING status. For recurring frequencies (WEEKLY/BIWEEKLY/MONTHLY), generates the full date series automatically.")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<ApiResponse<?>> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateOrderRequest req,
            HttpServletRequest httpReq) {

        String token = extractToken(httpReq);
        return orderService.createOrder(
                clientId(user), req.getPropertyId(), req.getServiceTypeCode(),
                req.getAddonCodes(), req.getFrequency(), req.getScheduledDate(),
                req.getNotes(), token
        ).fold(
                err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                series -> {
                    List<OrderResponse> responses = series.stream().map(OrderResponse::from).collect(Collectors.toList());
                    String msg = series.size() == 1
                            ? "Order created. Pay to confirm."
                            : String.format("Recurring series of %d orders created (%s). Pay the first order to confirm.",
                              series.size(), req.getFrequency());
                    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(msg, responses));
                }
        );
    }

    @Operation(summary = "Pay for a PENDING order",
               description = "Simulates payment. Changes PENDING → CONFIRMED and assigns a cleaner via Round-Robin.")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<ApiResponse<OrderResponse>> pay(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id) {
        return orderService.payOrder(id, clientId(user))
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                        o   -> ResponseEntity.ok(ApiResponse.ok(
                                "Payment confirmed! Cleaner assigned: " +
                                (o.getCleaner() != null ? o.getCleaner().getId() : "TBD"),
                                OrderResponse.from(o)))
                );
    }

    @Operation(summary = "My orders — flat list, sorted by date ascending")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> list(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.getClientOrders(clientId(user), PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "scheduledDate")))
                        .map(OrderResponse::from)));
    }

    @Operation(summary = "Get full series for an order")
    @GetMapping("/{id}/series")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> series(
            @AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {
        return orderService.getOrderSeries(id, clientId(user))
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        list -> ResponseEntity.ok(ApiResponse.ok(
                                list.stream().map(OrderResponse::from).collect(Collectors.toList())))
                );
    }

    @Operation(summary = "Get order details")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> get(
            @AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {
        return orderService.getClientOrder(id, clientId(user))
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        o -> ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(o)))
                );
    }

    @Operation(summary = "Cancel order")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return orderService.cancelOrder(id, clientId(user), reason)
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                        o -> ResponseEntity.ok(ApiResponse.ok("Cancelled", OrderResponse.from(o)))
                );
    }

    private UUID clientId(UserDetails user) {
        return (user instanceof com.project.order.security.UserPrincipal p)
            ? p.getUserId()
            : UUID.nameUUIDFromBytes(user.getUsername().getBytes()); // fallback
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    }
}
