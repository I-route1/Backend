package com.i_route.backend.auth;

import com.i_route.backend.auth.dto.LoginRequest;
import com.i_route.backend.auth.dto.AuthResponse;
import com.i_route.backend.auth.dto.ParentRegisterRequestDto;
import com.i_route.backend.auth.entity.EmailVerificationToken;
import com.i_route.backend.auth.entity.RefreshToken;
import com.i_route.backend.auth.repository.EmailVerificationTokenRepository;
import com.i_route.backend.auth.repository.RefreshTokenRepository;
import com.i_route.backend.auth.service.AuthService;
import com.i_route.backend.auth.service.KakaoOAuthService;
import com.i_route.backend.global.jwt.JwtUtil;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.AcademyRepository;
import com.i_route.backend.user.repository.UserRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private AcademyRepository academyRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private KakaoOAuthService kakaoOAuthService;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiration", 1209600000L);
        ReflectionTestUtils.setField(authService, "verificationExpiration", 3600L);
    }

    // ============================================================
    // 회원가입
    // ============================================================

    @Test
    @DisplayName("회원가입 성공 - 정상 입력")
    void signup_success() {
        ParentRegisterRequestDto req = new ParentRegisterRequestDto();
        req.setUsername("testuser");
        req.setEmail("test@test.com");
        req.setPassword("password123");
        req.setPasswordConfirm("password123");
        req.setNickname("테스터");
        req.setName("홍길동");
        req.setPhone("010-1234-5678");
        req.setRole("PARENT");

        given(userRepository.existsByUsername("testuser")).willReturn(false);
        given(userRepository.existsByNickname("테스터")).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(User.builder().id(1L).build());
        given(passwordEncoder.encode(anyString())).willReturn("encoded");

        assertThatNoException().isThrownBy(() -> authService.register(req));
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 잘못된 역할")
    void signup_invalidRole() {
        ParentRegisterRequestDto req = new ParentRegisterRequestDto();
        req.setUsername("testuser");
        req.setEmail("test@test.com");
        req.setPassword("password123");
        req.setPasswordConfirm("password123");
        req.setNickname("테스터");
        req.setName("홍길동");
        req.setPhone("01012345678");
        req.setRole("INVALID_ROLE");

        given(userRepository.existsByUsername(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 role");
    }

    @Test
    @DisplayName("회원가입 시 전화번호 하이픈 제거")
    void signup_phoneNumberNormalized() {
        ParentRegisterRequestDto req = new ParentRegisterRequestDto();
        req.setUsername("testuser");
        req.setEmail("test@test.com");
        req.setPassword("password123");
        req.setPasswordConfirm("password123");
        req.setNickname("테스터");
        req.setName("홍길동");
        req.setPhone("010-1234-5678");
        req.setRole("PARENT");

        given(userRepository.existsByUsername(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        then(userRepository).should().save(argThat(u -> "01012345678".equals(u.getPhoneNumber())));
    }

    // ============================================================
    // 로그인
    // ============================================================

    @Test
    @DisplayName("로그인 성공 - 토큰 포함 응답 반환")
    void login_success() {
        User user = User.builder().id(1L).email("test@test.com")
                .password("encoded").nickname("테스터")
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL).build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded")).willReturn(true);
        given(jwtUtil.generateToken(1L)).willReturn("access-token");
        given(jwtUtil.generateRefreshToken(1L)).willReturn("refresh-token");

        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("password123");

        AuthResponse resp = authService.login(req);

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
        assertThat(resp.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(resp.getNickname()).isEqualTo("테스터");
        assertThat(resp.getIsNewUser()).isFalse();
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_userNotFound() {
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("none@test.com");
        req.setPassword("password123");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("유저 없음");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword() {
        User user = User.builder().id(1L).email("test@test.com")
                .password("encoded").role(User.UserRole.PARENT)
                .loginType(User.LoginType.EMAIL).build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("비밀번호 틀림");
    }

    // ============================================================
    // 로그아웃
    // ============================================================

    @Test
    @DisplayName("로그아웃 성공 - refresh token 삭제")
    void logout_success() {
        authService.logout("some-refresh-token");
        then(refreshTokenRepository).should().deleteByToken("some-refresh-token");
    }

    // ============================================================
    // Access Token 재발급
    // ============================================================

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissueAccessToken_success() {
        RefreshToken saved = RefreshToken.builder()
                .token("valid-refresh")
                .userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();

        User u = User.builder().id(1L).nickname("테스터")
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL).build();

        given(refreshTokenRepository.findByToken("valid-refresh")).willReturn(Optional.of(saved));
        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(jwtUtil.generateToken(1L)).willReturn("new-access-token");

        AuthResponse resp = authService.reissueAccessToken("valid-refresh");

        assertThat(resp.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 존재하지 않는 refresh token")
    void reissueAccessToken_notFound() {
        given(refreshTokenRepository.findByToken(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissueAccessToken("invalid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 refresh token")
    void reissueAccessToken_expired() {
        RefreshToken expired = RefreshToken.builder()
                .token("expired-refresh")
                .userId(1L)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        given(refreshTokenRepository.findByToken("expired-refresh")).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.reissueAccessToken("expired-refresh"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("만료");
    }

    // ============================================================
    // 이메일 인증
    // ============================================================

    @Test
    @DisplayName("이메일 인증 성공")
    void verifyEmail_success() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("valid-token")
                .email("test@test.com")
                .expiryDate(LocalDateTime.now().plusHours(1))
                .verified(false)
                .build();

        User user = User.builder().id(1L).email("test@test.com")
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL).build();

        given(emailVerificationTokenRepository.findByToken("valid-token")).willReturn(Optional.of(token));
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        assertThatNoException().isThrownBy(() -> authService.verifyEmail("valid-token"));
        assertThat(token.isVerified()).isTrue();
    }

    @Test
    @DisplayName("이메일 인증 실패 - 이미 인증된 토큰")
    void verifyEmail_alreadyVerified() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("used-token")
                .email("test@test.com")
                .expiryDate(LocalDateTime.now().plusHours(1))
                .verified(true)
                .build();

        given(emailVerificationTokenRepository.findByToken("used-token")).willReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("used-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 인증");
    }

    @Test
    @DisplayName("이메일 인증 실패 - 만료된 토큰")
    void verifyEmail_expired() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("expired-token")
                .email("test@test.com")
                .expiryDate(LocalDateTime.now().minusHours(1))
                .verified(false)
                .build();

        given(emailVerificationTokenRepository.findByToken("expired-token")).willReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("만료");
    }

    // ============================================================
    // 비밀번호 재설정
    // ============================================================

    @Test
    @DisplayName("비밀번호 재설정 링크 발송 성공")
    void sendResetLink_success() {
        User user = User.builder().id(1L).email("test@test.com")
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL).build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        assertThatNoException().isThrownBy(() -> authService.sendResetLink("test@test.com"));
        assertThat(user.getResetToken()).isNotNull();
    }

    @Test
    @DisplayName("비밀번호 재설정 링크 발송 실패 - 미가입 이메일")
    void sendResetLink_notFound() {
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.sendResetLink("nobody@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가입되지 않은");
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 - 토큰 폐기 및 비밀번호 변경")
    void resetPassword_success() {
        User user = User.builder().id(1L).email("test@test.com")
                .password("old-encoded").resetToken("reset-token")
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL).build();

        given(userRepository.findByResetToken("reset-token")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPassword")).willReturn("new-encoded");

        authService.resetPassword("reset-token", "newPassword");

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        assertThat(user.getResetToken()).isNull();
    }

    // ============================================================
    // 이메일 찾기
    // ============================================================

    @Test
    @DisplayName("이메일 찾기 성공 - 전화번호 하이픈 포함")
    void findEmailByPhoneNumber_withHyphen() {
        User user = User.builder().id(1L).email("test@test.com")
                .phoneNumber("01012345678").role(User.UserRole.PARENT)
                .loginType(User.LoginType.EMAIL).build();

        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));

        String email = authService.findEmailByPhoneNumber("010-1234-5678");
        assertThat(email).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("이메일 찾기 실패 - 미등록 전화번호")
    void findEmailByPhoneNumber_notFound() {
        given(userRepository.findByPhoneNumber(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.findEmailByPhoneNumber("01099999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유저가 없습니다");
    }
}
