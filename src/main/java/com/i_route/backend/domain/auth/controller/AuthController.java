package com.i_route.backend.domain.auth.controller;

import com.i_route.backend.domain.auth.dto.*;
import com.i_route.backend.domain.auth.service.AuthService;
import com.i_route.backend.domain.auth.service.KakaoOAuthService;
import com.i_route.backend.domain.user.dto.DuplicateCheckResponse;
import com.i_route.backend.domain.user.dto.SingleCheckRequest;
import com.i_route.backend.domain.user.entity.User;
import com.i_route.backend.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KakaoOAuthService kakaoOAuthService;
    private final UserService userService;

    //일반
    @PostMapping("/api/auth/signup")
    public ResponseEntity<String> register(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    // 소셜
    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @GetMapping("/api/oauth/social/{provider}")
    public void socialLogin(
            @PathVariable String provider,
            HttpServletResponse response
    ) throws IOException {

        if (!provider.equals("kakao")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String url = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code";

        response.sendRedirect(url);
    }

    @GetMapping("/api/oauth/social/{provider}/token")
    public ResponseEntity<?> socialToken(
            @PathVariable String provider,
            @RequestParam("code") String code
    ) {

        System.out.println("받은 code = " + code);

        return ResponseEntity.ok(
                authService.kakaoLogin(code)
        );
    }

    // POST /api/oauth/social/{provider}/register → 소셜 회원 자동 가입
    @PostMapping("/api/oauth/social/{provider}/register")
    public ResponseEntity<Map<String, String>> socialRegister(
            @PathVariable String provider,
            @RequestBody SocialRegisterRequest request) {

        User user = authService.socialRegister(request);
        return ResponseEntity.ok(Map.of(
                "email", String.valueOf(user.getEmail()),
                "message", "소셜 회원가입이 완료되었습니다."
        ));
    }

    // POST /api/oauth/social/{provider}/link → 기존 계정 연동
    @PostMapping("/api/oauth/social/{provider}/link")
    public ResponseEntity<Map<String, String>> socialLink(
            @PathVariable String provider,
            @RequestBody Map<String, String> body) {

        authService.linkKakaoAccount(body.get("userId"), body.get("providerId"));
        return ResponseEntity.ok(Map.of("message", "카카오 계정이 연동되었습니다."));
    }

    // POST /api/auth/token/refresh
    @PostMapping("/api/auth/token/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> body) {
        AuthResponse response = authService.reissueAccessToken(body.get("refreshToken"));
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/logout
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        authService.logout(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    // POST /api/auth/email/send
    @PostMapping("/api/auth/email/send")
    public ResponseEntity<Map<String, String>> sendVerificationEmail(
            @RequestBody Map<String, String> body) {
        authService.sendVerificationEmail(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "인증 메일이 발송되었습니다."));
    }

    // POST /api/auth/email/resend
    @PostMapping("/api/auth/email/resend")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(
            @RequestBody Map<String, String> body) {
        authService.resendVerificationEmail(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "인증 메일이 재발송되었습니다."));
    }

    // GET /api/auth/email/verify?token=xxx
    @GetMapping("/api/auth/email/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
    }

    // POST /api/auth/email/welcome
    @PostMapping("/api/auth/email/welcome")
    public ResponseEntity<Map<String, String>> sendWelcomeEmail(
            @RequestBody Map<String, String> body) {
        authService.sendWelcomeEmail(body.get("userId"), body.get("email"));
        return ResponseEntity.ok(Map.of("message", "환영 이메일이 발송되었습니다."));
    }

    @PostMapping("/api/auth/check") // JSON을 보낼 때는 보통 POST를 많이 씁니다.
    public ResponseEntity<DuplicateCheckResponse> checkSingleField(@RequestBody SingleCheckRequest request) {

        if (request.getValue() == null || request.getValue().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new DuplicateCheckResponse(false, null, "검증할 값을 입력해주세요.")
            );
        }

        DuplicateCheckResponse response = userService.checkSingleField(request.getType(), request.getValue().trim());

        if (response == null) {
            return ResponseEntity.badRequest().body(
                    new DuplicateCheckResponse(false, null, "잘못된 검증 타입입니다. (email, nickname, phone 중 입력)")
            );
        }

        return ResponseEntity.ok(response);
    }

    // 비밀번호 재설정 링크 발송
    @PostMapping("/api/auth/password/reset/send")
    public ResponseEntity<MessageResponse> sendPasswordResetLink(@RequestBody PasswordResetSendRequest request) {
        authService.sendResetLink(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("비밀번호 재설정 메일이 발송되었습니다."));
    }

    // 비밀번호 재설정
    @PatchMapping("/api/auth/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody PasswordResetRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("비밀번호가 변경되었습니다."));
    }

    // 아이디 찾기
    @PostMapping("/api/auth/find/email")
    public ResponseEntity<FindEmailResponse> findEmail(@RequestBody FindEmailRequest request) {
        String email = authService.findEmailByPhoneNumber(request.getPhoneNumber());
        return ResponseEntity.ok(new FindEmailResponse(email));
    }
}
