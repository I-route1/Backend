package com.i_route.backend.auth.controller;

import com.i_route.backend.auth.dto.*;
import com.i_route.backend.auth.entity.EmailVerificationToken;
import com.i_route.backend.auth.repository.EmailVerificationTokenRepository;
import com.i_route.backend.auth.service.AuthService;
import com.i_route.backend.auth.service.EmailService;
import com.i_route.backend.global.response.ApiResponse;
import com.i_route.backend.user.dto.DuplicateCheckResponse;
import com.i_route.backend.user.dto.SingleCheckRequest;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    //일반
    @PostMapping("/api/auth/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody ParentRegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다.")
        );

    }

    @PostMapping("/api/auth/academy/register")
    public ResponseEntity<ApiResponse<String>> academyregister(@RequestBody AcademyRegisterRequestDto request) {
        authService.registerAcademy(request);
        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다.")
        );
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

    @GetMapping("/api/auth/email/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {

        System.out.println("====== VERIFY EMAIL REQUEST ======");
        System.out.println("TOKEN = " + token);

        try {
            ApiResponse<String> result = emailService.verifyEmail(token);

            System.out.println("SERVICE RESULT = " + result);
            System.out.println("SUCCESS = " + result.isSuccess());

            if (result.isSuccess()) {
                System.out.println("RETURN 200 OK");
                return ResponseEntity.ok(result);
            }

            System.out.println("RETURN 400 BAD REQUEST");
            return ResponseEntity.badRequest().body(result);

        } catch (Exception e) {
            System.out.println("====== VERIFY EMAIL ERROR ======");
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
        }
    }


    @PostMapping("/api/auth/email/welcome")
    public ResponseEntity<Map<String, String>> sendWelcomeEmail(
            @RequestBody Map<String, String> body) {

        authService.sendWelcomeEmail(body.get("email"));

        return ResponseEntity.ok(
                Map.of("message", "환영 이메일이 발송되었습니다.")
        );
    }

    @PostMapping("/api/auth/check")
    public ResponseEntity<DuplicateCheckResponse> checkSingleField(
            @RequestBody SingleCheckRequest request
    ) {
        System.out.println("====== 중복 체크 요청 ======");
        System.out.println("Type: " + request.getType());
        System.out.println("Value: " + request.getValue());

        // ✅ null 방어 (request 자체도)
        if (request == null || request.getValue() == null || request.getValue().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new DuplicateCheckResponse(true, null, "검증할 값을 입력해주세요.")
            );
        }

        String type = request.getType();
        String value = request.getValue().trim();

        // ✅ type null 방어
        if (type == null) {
            return ResponseEntity.badRequest().body(
                    new DuplicateCheckResponse(true, null, "검증 타입이 없습니다.")
            );
        }

        // ✅ type 검증
        if (!List.of("username", "nickname", "email", "phoneNumber").contains(type)) {
            return ResponseEntity.badRequest().body(
                    new DuplicateCheckResponse(true, null, "잘못된 검증 타입입니다.")
            );
        }

        // ✅ 전화번호 normalize (완전 정규화)
        if ("phoneNumber".equals(type)) {
            value = value.replaceAll("\\D", "");
        }

        DuplicateCheckResponse response = userService.checkSingleField(type, value);

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

    // 아이디/이메일 찾기
    @PostMapping("/api/auth/find/username-email")
    public ResponseEntity<FindUsernameAndEmailResponse> findUsernameAndEmail(
            @RequestBody FindUsernameAndEmailRequest request
    ) {
        FindUsernameAndEmailResponse response =
                authService.findUsernameAndEmailByPhoneNumber(request.getPhoneNumber());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/auth/email/status")
    public ResponseEntity<?> checkEmailStatus(@RequestParam String email) {

        System.out.println("====== EMAIL STATUS CHECK ======");
        System.out.println("input email = " + email);

        EmailVerificationToken token =
                emailVerificationTokenRepository
                        .findTopByEmailOrderByIdDesc(email.trim().toLowerCase())
                        .orElse(null);

        System.out.println("LATEST TOKEN = " + token);

        if (token != null) {
            System.out.println("TOKEN EMAIL = " + token.getEmail());
            System.out.println("TOKEN VERIFIED = " + token.isVerified());
            System.out.println("TOKEN ID = " + token.getId());
        }

        boolean verified = token != null && token.isVerified();

        System.out.println("FINAL RESULT = " + verified);

        return ResponseEntity.ok(Map.of("verified", verified));
    }
}
