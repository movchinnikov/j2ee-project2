package com.project.iac.web.controller;

import com.project.iac.service.UserService;
import com.project.iac.web.dto.response.ApiResponse;
import com.project.iac.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User management endpoints.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Users", description = "User profile and management")
public class UserController {

    private final UserService userService;

    // ── GET /me ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {

        return userService.getByUsername(userDetails.getUsername())
                .fold(
                        error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error)),
                        user -> ResponseEntity.ok(ApiResponse.ok(user))
                );
    }

    // ── GET / (paginated) ───────────────────────────────────────────────────

    @Operation(summary = "List all users (Admin+)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction dir = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Users fetched", users));
    }

    // ── GET /{id} ───────────────────────────────────────────────────────────

    @Operation(summary = "Get user by ID (Admin+)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable UUID id) {

        return userService.getById(id)
                .fold(
                        error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error)),
                        user -> ResponseEntity.ok(ApiResponse.ok(user))
                );
    }

    // ── DELETE /{id} ────────────────────────────────────────────────────────

    @Operation(summary = "Delete user (Super Admin only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        return userService.deleteUser(id)
                .fold(
                        error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(error)),
                        ignored -> ResponseEntity.ok(ApiResponse.ok("User deleted", null))
                );
    }
}
