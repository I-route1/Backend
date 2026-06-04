package com.i_route.backend.auth.service;

import com.i_route.backend.auth.dto.*;
import com.i_route.backend.auth.repository.RefreshTokenRepository;
import com.i_route.backend.auth.entity.EmailVerificationToken;
import com.i_route.backend.auth.entity.RefreshToken;
import com.i_route.backend.board.repository.*;
import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.*;
import com.i_route.backend.global.jwt.JwtUtil;
import com.i_route.backend.auth.repository.EmailVerificationTokenRepository;
import com.i_route.backend.global.exception.CustomException;
import com.i_route.backend.global.exception.ErrorCode;

import lombok.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private final AcademyRepository academyRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final KakaoOAuthService kakaoOAuthService;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    private final JavaMailSender mailSender;

    private final CommentRepository commentRepository;

    private final CommentLikeRepository commentLikeRepository;

    private final PostBookmarkRepository postBookmarkRepository;

    private final PostLikeRepository postLikeRepository;

    private final PostRepository postRepository;

    private final JwtUtil jwtUtil;
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }

        String normalized = phoneNumber.replaceAll("[\\s-]", "");

        return normalized.isBlank() ? null : normalized;
    }
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
        User user = userRepository.findById(saved.getUserId())
                .orElseThrow(() -> new RuntimeException("유저 없음"));
        String newAccessToken = jwtUtil.generateToken(user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .role(user.getRole().name())
                .build();
    }

    public void register(ParentRegisterRequestDto request) {

        EmailVerificationToken token =
                emailVerificationTokenRepository
                        .findTopByEmailOrderByIdDesc(request.getEmail())
                        .orElse(null);

        if (token == null || !Boolean.TRUE.equals(token.isVerified())) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }
        // 아이디 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 이메일 중복 체크
        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail())) {

            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 확인
        if (!request.getPassword()
                .equals(request.getPasswordConfirm())) {

            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 역할 검증
        User.UserRole role;
        try {
            role = User.UserRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 role 값입니다: " + request.getRole());
        }

        // 회원 생성
        User user = User.builder()
                .username(request.getUsername())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(normalizeBlankToNull(request.getEmail()))
                .phoneNumber(normalizePhoneNumber(request.getPhoneNumber()))
                .role(role)
                .loginType(User.LoginType.EMAIL)
                .build();

        userRepository.save(user);
        emailVerificationTokenRepository.deleteByEmail(request.getEmail());
    }

    @Transactional
    public void registerAcademy(AcademyRegisterRequestDto request) {

        EmailVerificationToken token =
                emailVerificationTokenRepository
                        .findTopByEmailOrderByIdDesc(request.getEmail())
                        .orElse(null);

        if (token == null || !Boolean.TRUE.equals(token.isVerified())) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }

        // 아이디 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 이메일 중복 체크
        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail())) {

            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 확인
        if (!request.getPassword()
                .equals(request.getPasswordConfirm())) {

            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // role 안전 처리
        User.UserRole role;
        try {
            role = User.UserRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 role 값입니다: " + request.getRole());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .name(request.getName())
                .email(normalizeBlankToNull(request.getEmail()))
                .role(role)
                .phoneNumber(normalizePhoneNumber(request.getPhoneNumber()))
                .loginType(User.LoginType.EMAIL)
                .build();

        userRepository.save(user);

        Academy.AcademyBuilder academyBuilder = Academy.builder()
                .academyName(request.getAcademyName())
                .user(user);

        if (role == User.UserRole.ACADEMY) {
            academyBuilder
                    .academyAddress(request.getAcademyAddress())
                    .businessNumber(request.getBusinessNumber());
        }

        if (role == User.UserRole.DRIVER) {
            academyBuilder
                    .vehicleNumber(request.getVehicleNumber())
                    .inviteCode(request.getInviteCode());
        }

        academyRepository.save(academyBuilder.build());
        emailVerificationTokenRepository.deleteByEmail(request.getEmail());
    }

    // return 타입을 LoginResponse로 변경
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtUtil.generateToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(String.valueOf(user.getId()))
                .nickname(user.getNickname())
                .role(user.getRole().name())
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
                .role(user.getRole().name())
                .isNewUser(isNewUser)
                .build();
    }

    public User socialRegister(SocialRegisterRequest request) {
        String providerId = request.getProviderId();

        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("카카오 providerId가 없습니다.");
        }

        String nickname = request.getNickname();

        if (nickname == null || nickname.isBlank()) {
            nickname = "카카오회원_" + providerId;
        } else {
            nickname = "kakao_" + nickname;
        }

        String email = normalizeBlankToNull(request.getEmail());

        if (email == null) {
            email = "kakao_" + providerId + "@kakao.local";
        }

        return userRepository.save(
                User.builder()
                        .kakaoId(Long.valueOf(providerId))
                        .name(nickname)
                        .nickname(nickname)
                        .email(email)
                        .username("kakao_" + providerId)
                        .phoneNumber("KAKAO_" + providerId)
                        .password("kakao_" + providerId)
                        .role(User.UserRole.PARENT)
                        .loginType(User.LoginType.KAKAO)
                        .emailVerified(true)
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


    public void sendVerificationEmail(String email) {

        // 토큰 생성
        emailVerificationTokenRepository.deleteByEmail(email);
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verification = EmailVerificationToken.builder()
                .email(email)
                .token(token)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .verified(false)
                .build();
        emailVerificationTokenRepository.save(verification);

        // 인증 링크 생성
        String verifyUrl =
                frontendBaseUrl + "/email/verify?token=" + token;

        // 메일 생성
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("[I-ROUTE] 이메일 인증");

        message.setText(
                "아래 링크를 클릭해서 인증을 완료해주세요.\n\n"
                        + verifyUrl
        );

        // 메일 발송
        mailSender.send(message);
    }


    //  인증 이메일 재발송
    public void resendVerificationEmail(String email) {
        sendVerificationEmail(email); // 기존 토큰 삭제 후 재발송
    }

    // 비밀번호 재설정 링크 발송 로직
    @Transactional
    public void sendResetLink(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("가입되지 않은 이메일입니다."));

        String resetToken = UUID.randomUUID().toString();

        user.updateResetToken(resetToken);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + resetToken;

        System.out.println("emailBaseUrl = " + frontendBaseUrl);
        System.out.println("resetLink = " + resetLink);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[I-ROUTE] 비밀번호 재설정 " + LocalDateTime.now());

        message.setText(
                "비밀번호 재설정 링크입니다.\n\n" +
                        resetLink
        );

        mailSender.send(message);
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

    @Transactional(readOnly = true)
    public FindUsernameAndEmailResponse findUsernameAndEmailByPhoneNumber(String phone) {
        String cleanedPhone = phone.replaceAll("[\\s-]", "");

        User user = userRepository.findByPhoneNumber(cleanedPhone)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 휴대폰 번호로 가입된 유저가 없습니다.")
                );

        return new FindUsernameAndEmailResponse(
                user.getUsername(),
                user.getEmail()
        );
    }

    public void sendWelcomeEmail(String email) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("[I-ROUTE] 회원가입을 환영합니다!");
        message.setText(
                "안녕하세요.\n\n" +
                        "I-ROUTE 회원가입을 환영합니다!\n" +
                        "앞으로 다양한 서비스를 이용해보세요 😊"
        );

        mailSender.send(message);
    }

    @Transactional
    public void changePassword(
            Long userId,
            String currentPassword,
            String newPassword
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(
                currentPassword,
                user.getPassword()
        )) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );
    }

    @Transactional
    public void deleteMyAccount(Long userId) {

        // 댓글 관련

        commentRepository.deleteByUser_Id(userId);

        // 게시글에 달린 반응 삭제
        postBookmarkRepository.deleteByUser_Id(userId);
        postLikeRepository.deleteByUser_Id(userId);

        // 내가 쓴 게시글에 달린 반응/댓글 삭제
        commentLikeRepository.deleteByPostOwnerId(userId);

        commentRepository.deleteByPost_User_Id(userId);

        postBookmarkRepository.deleteByPost_User_Id(userId);
        postLikeRepository.deleteByPost_User_Id(userId);

        // 게시글 삭제
        postRepository.deleteByUser_Id(userId);

        // 회원 부가정보 삭제
        academyRepository.deleteByUser_Id(userId);

        // 마지막 회원 삭제
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userRepository.delete(user);
    }

}