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

    public static <T> com.i_route.backend.global.response.ApiResponse<T> success(T data) {
        return com.i_route.backend.global.response.ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("요청에 성공했습니다.")
                .data(data)
                .build();
    }

    public static <T> com.i_route.backend.global.response.ApiResponse<T> fail(
            String code,
            String message
    ) {
        return com.i_route.backend.global.response.ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}

