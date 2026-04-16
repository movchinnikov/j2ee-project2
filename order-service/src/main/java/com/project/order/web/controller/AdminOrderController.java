package com.project.order.web.controller;

import com.project.order.service.OrderService;
import com.project.order.web.dto.response.ApiResponse;
import com.project.order.web.dto.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only order management endpoints.
 * Requires ROLE_ADMIN or ROLE_SUPER_ADMIN (enforced in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin — Orders", description = "Admin-only order management")
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "Get all orders (paginated, optional status filter)")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false)    String status) {

        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OrderResponse> result = (status != null && !status.isBlank())
                ? orderService.getAllOrdersByStatus(status.toUpperCase(), pr).map(OrderResponse::from)
                : orderService.getAllOrders(pr).map(OrderResponse::from);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
