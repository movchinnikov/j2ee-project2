package com.project.iac.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for assigning or revoking a role.
 */
@Data
@Schema(description = "Role assignment request")
public class AssignRoleRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Target username", example = "john_doe")
    private String username;

    @NotBlank(message = "Role name is required")
    @Schema(description = "Role to assign (ADMIN, USER, SUPER_ADMIN)", example = "ADMIN")
    private String roleName;
}
