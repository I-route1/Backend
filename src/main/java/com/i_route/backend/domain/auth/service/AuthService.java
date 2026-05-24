package com.i_route.backend.domain.auth.service;

import com.i_route.backend.domain.auth.dto.*;
import com.i_route.backend.domain.auth.entity.EmailVerificationToken;
import com.i_route.backend.domain.auth.entity.RefreshToken;
import com.i_route.backend.domain.auth.repository.*;
import com.i_route.backend.domain.user.entity.User;
import com.i_route.backend.domain.user.repository.*;
import com.i_route.backend.global.jwt.JwtUtil;
import com.i_route.backend.domain.auth.repository.EmailVerificationTokenRepository;
import com.nimbusds.oauth2.sdk.TokenResponse;

import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional

@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final KakaoOAuthService kakaoOAuthService;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    private final EmailService emailService;

    private final JwtUtil jwtUtil;
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private String issueRefreshToken(Long userId) {
        // 기존 토큰 삭제 (중복 방지)
        refreshTokenRepository.deleteByUserId(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshToken)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build());
        return refreshToken;
    }
    // ✅ Access Token 재발급
    public AuthResponse reissueAccessToken(String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 Refresh Token입니다."));
        if (saved.isExpired()) {
            refreshTokenRepository.delete(saved);
            throw new RuntimeException("Refresh Token이 만료되었습니다. 다시 로그인해주세요.");
        }
        String newAccessToken = jwtUtil.generateToken(saved.getUserId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @Transactional
    public void signup(SignupRequest request) {
        // 1. 중복 체크 등 기존 검증 로직 수행...

        // 2. 입력받은 role 문자열을 Enum으로 안전하게 변환
        User.UserRole userRole;
        try {
            userRole = User.UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("올바른 역할을 선택해주세요. (PARENT, TEACHER, DRIVER)");
        }

        // 3. 휴대폰 번호 하이픈 제거 포맷팅
        String cleanedPhone = request.getPhoneNumber().replaceAll("[\\s-]", "");

        // 4. 엔티티 생성 및 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // 비밀번호 암호화
                .nickname(request.getNickname())
                .phoneNumber(cleanedPhone)
                .role(userRole) // 👈 변환된 Enum 역할 저장!
                .loginType(User.LoginType.EMAIL)
                .build();

        userRepository.save(user);
    }

    // return 타입을 LoginResponse로 변경
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        System.out.println("====== [로그인 디버깅 데이터] ======");
        System.out.println("1. 포스트맨이 보낸 원본 패스워드 : [" + request.getPassword() + "]");
        System.out.println("2. DB에서 꺼내온 암호화 패스워드 : [" + user.getPassword() + "]");
        System.out.println("==================================");

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호 틀림");
        }

        String accessToken = jwtUtil.generateToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        String userId;
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(String.valueOf(user.getId()))
                .nickname(user.getNickname())
                .isNewUser(false)
                .build();
    }

    // ✅ 로그아웃 - Refresh Token 무효화
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }
    // ✅ 카카오 로그인 - Refresh Token 포함
    public AuthResponse kakaoLogin(String code) {
        String kakaoAccessToken = kakaoOAuthService.getAccessToken(code);
        KakaoUserInfoResponse userInfo = kakaoOAuthService.getUserInfo(kakaoAccessToken);
        boolean isNewUser = userRepository.findByKakaoId(userInfo.getId()).isEmpty();
        User user = userRepository.findByKakaoId(userInfo.getId())
                .orElseGet(() -> userRepository.save(
                        User.ofKakao(userInfo.getId(), userInfo.getNickname(), null)
                ));
        String accessToken = jwtUtil.generateToken(user.getId());        String refreshToken = issueRefreshToken(user.getId());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(String.valueOf(user.getId()))
                .nickname(user.getNickname())
                .isNewUser(isNewUser)
                .build();
    }

    // 소셜 회원 자동 가입
    public User socialRegister(SocialRegisterRequest request) {
        return userRepository.save(
                User.builder()
                        .kakaoId(Long.parseLong(request.getProviderId()))
                        .nickname(request.getNickname())
                        .email(request.getEmail())
                        .role(User.UserRole.valueOf("USER"))
                        .loginType(User.LoginType.KAKAO)
                        .build()
        );
    }

    // 기존 계정 카카오 연동
    public void linkKakaoAccount(String userId, String providerId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        user.setKakaoId(Long.parseLong(providerId));
        userRepository.save(user);
    }

    @Value("${email.verification-expiration}")
    private long verificationExpiration;

    // 인증 이메일 발송
    public void sendVerificationEmail(String email) {
        // 기존 토큰 삭제
        emailVerificationTokenRepository.deleteByEmail(email);

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .token(token)
                        .email(email)
                        .expiryDate(LocalDateTime.now().plusHours(1))
                        .verified(false)
                        .build();

        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(email, token);
    }

    //  인증 토큰 검증
    public void verifyEmail(String token) {

        EmailVerificationToken verification =
                emailVerificationTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException("유효하지 않은 토큰입니다."));

        if (verification.isVerified()) {
            throw new RuntimeException("이미 인증된 이메일입니다.");
        }

        if (verification.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("만료된 토큰입니다.");
        }

        verification.setVerified(true);

        User user = userRepository
                .findByEmail(verification.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setEmailVerified(true);

        emailVerificationTokenRepository.save(verification);
        userRepository.save(user);
    }

    //  인증 이메일 재발송
    public void resendVerificationEmail(String email) {
        sendVerificationEmail(email); // 기존 토큰 삭제 후 재발송
    }

    // 환영 이메일 발송
    public void sendWelcomeEmail(String userId, String email) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        emailService.sendWelcomeEmail(email, user.getNickname());
    }

    // 비밀번호 재설정 링크 발송 로직
    @Transactional
    public void sendResetLink(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 실무 팁: 토큰을 생성해 DB 유저 테이블(또는 Redis)에 저장하고 이메일 전송 로직을 붙입니다.
        String resetToken = UUID.randomUUID().toString();
        user.updateResetToken(resetToken); // User 엔티티에 토큰 저장 메서드가 있다고 가정

        // TODO: emailService.send(email, "비밀번호 재설정 링크", "링크주소?token=" + resetToken);
    }

    // 토큰 검증 후 새 비밀번호로 변경 로직
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 토큰으로 유저 찾기 (UserRepository에 findByResetToken 메서드 필요)
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다."));

        // 새 비밀번호 암호화 후 변경 및 토큰 초기화
        String encryptedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encryptedPassword);
        user.updateResetToken(null); // 사용한 토큰은 폐기
    }

    // 휴대폰 번호로 가입된 이메일 조회 로직
    @Transactional(readOnly = true)
    public String findEmailByPhoneNumber(String phone) {
        // 하이픈 제거 포맷팅
        String cleanedPhone = phone.replaceAll("[\\s-]", "");

        User user = userRepository.findByPhoneNumber(cleanedPhone)
                .orElseThrow(() -> new IllegalArgumentException("해당 휴대폰 번호로 가입된 유저가 없습니다."));

        return user.getEmail();
    }
}