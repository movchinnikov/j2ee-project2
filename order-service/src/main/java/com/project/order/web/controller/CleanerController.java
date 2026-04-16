package com.project.order.web.controller;

import com.project.order.service.OrderService;
import com.project.order.web.dto.response.ApiResponse;
import com.project.order.web.dto.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cleaner")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CLEANER')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Cleaner", description = "Cleaner schedule, earnings and order management")
public class CleanerController {

    private final OrderService orderService;

    /**
     * Extracts the real IAC user UUID from the JWT principal.
     * Also auto-initializes CleanerProfile if it doesn't exist (idempotent).
     */
    private UUID cleanerUserId(UserDetails user) {
        UUID uid = (user instanceof com.project.order.security.UserPrincipal p)
            ? p.getUserId()
            : UUID.nameUUIDFromBytes(user.getUsername().getBytes()); // fallback for old tokens
        orderService.getOrCreateProfile(uid);
        return uid;
    }

    @Operation(summary = "My upcoming cleaning schedule")
    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> schedule(@AuthenticationPrincipal UserDetails user) {
        return orderService.getCleanerSchedule(cleanerUserId(user))
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        list -> ResponseEntity.ok(ApiResponse.ok(list.stream().map(OrderResponse::from).toList()))
                );
    }

    @Operation(summary = "Mark order as completed (+recording earning)")
    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(
            @AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {
        return orderService.completeOrder(id, cleanerUserId(user))
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                        o -> ResponseEntity.ok(ApiResponse.ok("Completed", OrderResponse.from(o)))
                );
    }

    @Operation(summary = "Request replacement for an order")
    @PostMapping("/orders/{id}/request-replacement")
    public ResponseEntity<ApiResponse<OrderResponse>> requestReplacement(
            @AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {
        return orderService.requestReplacement(id, cleanerUserId(user))
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                        o -> ResponseEntity.ok(ApiResponse.ok("Replacement requested", OrderResponse.from(o)))
                );
    }

    @Operation(summary = "Toggle my availability")
    @PutMapping("/profile/availability")
    public ResponseEntity<ApiResponse<String>> setAvailability(
            @AuthenticationPrincipal UserDetails user, @RequestParam boolean available) {
        orderService.setAvailability(cleanerUserId(user), available);
        return ResponseEntity.ok(ApiResponse.ok("Availability set to: " + available));
    }

    @Operation(summary = "Total earnings")
    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<BigDecimal>> earnings(@AuthenticationPrincipal UserDetails user) {
        return orderService.getTotalEarnings(cleanerUserId(user))
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        amount -> ResponseEntity.ok(ApiResponse.ok("Total earnings", amount))
                );
    }

    @Operation(summary = "Monthly earnings (e.g. /earnings/2025/5 = May 2025)")
    @GetMapping("/earnings/{year}/{month}")
    public ResponseEntity<ApiResponse<BigDecimal>> monthlyEarnings(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable int year, @PathVariable int month) {
        return orderService.getMonthlyEarnings(cleanerUserId(user), year, month)
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        amount -> ResponseEntity.ok(ApiResponse.ok("Monthly total", amount))
                );
    }
}
