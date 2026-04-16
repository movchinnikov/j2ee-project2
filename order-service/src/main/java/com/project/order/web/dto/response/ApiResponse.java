package com.project.order.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T data)           { return ApiResponse.<T>builder().success(true).message("OK").data(data).build(); }
    public static <T> ApiResponse<T> ok(String m, T data) { return ApiResponse.<T>builder().success(true).message(m).data(data).build(); }
    public static <T> ApiResponse<T> error(String m)      { return ApiResponse.<T>builder().success(false).message(m).error(m).build(); }
}
