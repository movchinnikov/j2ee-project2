package com.project.booking.web.controller;

import com.project.booking.domain.entity.CleanerProfileEntity;
import com.project.booking.service.EarningService;
import com.project.booking.service.OrderService;
import com.project.booking.service.UnitOfWork;
import com.project.booking.web.dto.response.ApiResponse;
import com.project.booking.web.dto.response.EarningResponse;
import com.project.booking.web.dto.response.OrderResponse;
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
@Tag(name = "Cleaner", description = "Schedule, order management and earnings for cleaners")
public class CleanerController {

    private final OrderService orderService;
    private final EarningService earningService;
    private final UnitOfWork uow;

    // ── Profile auto-create on first access ──────────────────────────────

    private UUID resolveCleanerUserId(UserDetails user) {
        UUID userId = UUID.nameUUIDFromBytes(user.getUsername().getBytes());
        // Auto-create cleaner profile if it doesn't exist
        uow.getCleanerProfiles().findByUserId(userId).onEmpty(() -> {
            CleanerProfileEntity profile = CleanerProfileEntity.builder()
                    .userId(userId).isAvailable(true).build();
            uow.getCleanerProfiles().save(profile);
        });
        return userId;
    }

    // ── Schedule ──────────────────────────────────────────────────────────

    @Operation(summary = "My upcoming cleaning schedule")
    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getSchedule(
            @AuthenticationPrincipal UserDetails user) {

        return orderService.getCleanerSchedule(resolveCleanerUserId(user))
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        list -> ResponseEntity.ok(ApiResponse.ok("Schedule fetched", list))
                );
    }

    // ── Complete order ────────────────────────────────────────────────────

    @Operation(summary = "Mark order as completed")
    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id) {

        return orderService.completeOrder(id, resolveCleanerUserId(user))
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                        o -> ResponseEntity.ok(ApiResponse.ok("Order marked as completed", o))
                );
    }

    // ── Request replacement ───────────────────────────────────────────────

    @Operation(summary = "Request replacement for an order")
    @PostMapping("/orders/{id}/request-replacement")
    public ResponseEntity<ApiResponse<OrderResponse>> requestReplacement(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id) {

        return orderService.requestReplacement(id, resolveCleanerUserId(user))
                .fold(
                        err -> ResponseEntity.badRequest().body(ApiResponse.error(err)),
                        o -> ResponseEntity.ok(ApiResponse.ok("Replacement requested", o))
                );
    }

    // ── Availability ──────────────────────────────────────────────────────

    @Operation(summary = "Toggle availability")
    @PutMapping("/profile/availability")
    public ResponseEntity<ApiResponse<String>> setAvailability(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam boolean available) {

        UUID userId = resolveCleanerUserId(user);
        uow.getCleanerProfiles().findByUserId(userId).forEach(p -> {
            p.setAvailable(available);
            uow.getCleanerProfiles().save(p);
        });
        return ResponseEntity.ok(ApiResponse.ok("Availability updated to: " + available));
    }

    // ── Earnings ──────────────────────────────────────────────────────────

    @Operation(summary = "My earnings summary")
    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<EarningResponse.Summary>> getEarnings(
            @AuthenticationPrincipal UserDetails user) {

        return earningService.getSummary(resolveCleanerUserId(user))
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        s -> ResponseEntity.ok(ApiResponse.ok(s))
                );
    }

    @Operation(summary = "Monthly earnings", description = "e.g. /earnings/2025/5 = May 2025")
    @GetMapping("/earnings/{year}/{month}")
    public ResponseEntity<ApiResponse<BigDecimal>> getMonthlyEarnings(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable int year, @PathVariable int month) {

        return earningService.getMonthlyTotal(resolveCleanerUserId(user), year, month)
                .fold(
                        err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(err)),
                        amount -> ResponseEntity.ok(ApiResponse.ok("Monthly total", amount))
                );
    }
}
