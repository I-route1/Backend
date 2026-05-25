package com.i_route.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 공통 메시지 Response (재설정 완료, 발송 완료용)
@Getter
@AllArgsConstructor
public class MessageResponse {
    private String message;
}
