package com.project.iac.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper.
 * All endpoints return this structure for consistent error handling on the client side.
 *
 * @param <T> the data payload type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Whether the request succeeded", example = "true")
    private boolean success;

    @Schema(description = "Human-readable message", example = "User registered successfully")
    private String message;

    @Schema(description = "Response payload")
    private T data;

    @Schema(description = "Error details, present only on failure")
    private String error;

    @Builder.Default
    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Static factories ──────────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("OK", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String detail) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(detail)
                .build();
    }
}
