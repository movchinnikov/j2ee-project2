package com.project.iac.web.controller;

import com.project.iac.service.RoleService;
import com.project.iac.web.dto.request.AssignRoleRequest;
import com.project.iac.web.dto.response.ApiResponse;
import com.project.iac.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints — role management.
 * All endpoints require ADMIN or SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin", description = "Role assignment and administrative operations")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    private final RoleService roleService;

    // ── GET /roles ──────────────────────────────────────────────────────────

    @Operation(summary = "List all available roles")
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<String>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok("Roles fetched", roleService.getAllRoles()));
    }

    // ── POST /assign-role ───────────────────────────────────────────────────

    @Operation(
            summary = "Assign a role to a user",
            description = "Only ADMIN and SUPER_ADMIN can assign roles. SUPER_ADMIN role can only be assigned by SUPER_ADMIN."
    )
    @PostMapping("/assign-role")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @Valid @RequestBody AssignRoleRequest request) {

        return roleService.assignRole(request.getUsername(), request.getRoleName())
                .fold(
                        error -> ResponseEntity.badRequest().body(ApiResponse.error("Role assignment failed", error)),
                        user -> ResponseEntity.ok(ApiResponse.ok("Role assigned successfully", user))
                );
    }

    // ── POST /revoke-role ───────────────────────────────────────────────────

    @Operation(
            summary = "Revoke a role from a user",
            description = "SUPER_ADMIN role cannot be revoked."
    )
    @PostMapping("/revoke-role")
    public ResponseEntity<ApiResponse<UserResponse>> revokeRole(
            @Valid @RequestBody AssignRoleRequest request) {

        return roleService.revokeRole(request.getUsername(), request.getRoleName())
                .fold(
                        error -> ResponseEntity.badRequest().body(ApiResponse.error("Role revocation failed", error)),
                        user -> ResponseEntity.ok(ApiResponse.ok("Role revoked successfully", user))
                );
    }
}
