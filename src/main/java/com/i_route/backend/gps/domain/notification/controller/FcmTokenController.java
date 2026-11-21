package com.i_route.backend.gps.domain.notification.controller;

import com.i_route.backend.gps.domain.notification.dto.FcmTokenRequest;
import com.i_route.backend.gps.domain.notification.service.FcmTokenService;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;
    private final UserRepository userRepository;

    @PostMapping("/tokens")
    public ResponseEntity<Map<String, String>> saveToken(
            @Valid @RequestBody FcmTokenRequest request,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof com.i_route.backend.global.security.CustomUserDetails)) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "인증이 필요합니다."));
        }

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "인증된 사용자를 찾을 수 없습니다."
                        )
                );

        fcmTokenService.saveToken(
                user.getId(),
                request.token()
        );

        return ResponseEntity.ok(
                Map.of("message", "FCM 토큰 등록 성공")
        );
    }
}
