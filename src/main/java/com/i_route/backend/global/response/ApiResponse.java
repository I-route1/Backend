package com.i_route.backend.global.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;

    // 성공
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("요청에 성공했습니다.")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    // 실패 (핵심 개선)
    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code("FAIL")
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}