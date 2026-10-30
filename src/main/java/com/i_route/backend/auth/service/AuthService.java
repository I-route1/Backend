package com.i_route.backend.auth.service;

import com.i_route.backend.auth.dto.*;
import com.i_route.backend.auth.repository.RefreshTokenRepository;
import com.i_route.backend.auth.entity.EmailVerificationToken;
import com.i_route.backend.auth.entity.RefreshToken;
import com.i_route.backend.board.repository.*;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
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

    private final StudentRepository studentRepository;

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
    // AuthService.java 내 kakaoLogin 메서드

    public AuthResponse kakaoLogin(String code) {
        String kakaoAccessToken = kakaoOAuthService.getAccessToken(code);
        KakaoUserInfoResponse userInfo = kakaoOAuthService.getUserInfo(kakaoAccessToken);

        Long kakaoId = userInfo.getId();
        boolean isNewUser = userRepository.findByKakaoId(kakaoId).isEmpty();

        User user = userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .kakaoId(kakaoId)
                                .username("kakao_" + kakaoId)
                                .nickname(userInfo.getNickname() != null ? userInfo.getNickname() : "카카오_임시유저")
                                .name(userInfo.getNickname() != null ? userInfo.getNickname() : "카카오_임시유저")
                                .email(normalizeBlankToNull(userInfo.getEmail()))
                                // 💡 전화번호가 없으면 임시 번호 지정 (또는 null)
                                .phoneNumber(userInfo.getPhoneNumber() != null ?
                                        normalizePhoneNumber(userInfo.getPhoneNumber()) : "01000000000")
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .role(User.UserRole.PARENT)
                                .loginType(User.LoginType.KAKAO)
                                .emailVerified(true)
                                .build()
                ));

        String accessToken = jwtUtil.generateToken(user.getId());
        String refreshToken = issueRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(String.valueOf(user.getId()))
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .isNewUser(isNewUser) // 👈 신규 유저 여부 반환
                .build();
    }
    // 2. 소셜 가입 후 추가 정보 및 역할(Role) 선택 업데이트
    // AuthService.java 내 socialRegister 메서드

    @Transactional
    public User socialRegister(SocialRegisterRequest request) {
        String providerId = request.getProviderId();

        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("카카오 providerId가 없습니다.");
        }

        Long kakaoId = Long.parseLong(providerId);

        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new IllegalArgumentException("가입된 소셜 유저를 찾을 수 없습니다."));

        // 프론트에서 사용자가 선택한 Role 반영
        User.UserRole role = User.UserRole.PARENT;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            role = User.UserRole.valueOf(request.getRole().toUpperCase());
        }

        // 사용자가 입력한 전화번호 정규화 (숫자만 추출)
        String inputPhone = normalizePhoneNumber(request.getPhoneNumber());

        // 💡 사용자가 폼에 입력한 전화번호가 있으면 그것으로 업데이트하고, 없으면 기존 임시 번호 유지
        String finalPhone = (inputPhone != null && !inputPhone.isBlank()) ? inputPhone : user.getPhoneNumber();
        String finalName = (request.getName() != null && !request.getName().isBlank()) ? request.getName() : user.getName();
        String finalNickname = (request.getNickname() != null && !request.getNickname().isBlank()) ? request.getNickname() : user.getNickname();

        // 엔티티 업데이트
        user.updateSocialProfile(finalName, finalNickname, finalPhone, role);

        return user;
    }
    // 3. 기존 계정에 카카오 연동 (타입 안정성 및 중복 연동 방지)
    public void linkKakaoAccount(Long userId, String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId가 올바르지 않습니다.");
        }

        Long kakaoId;
        try {
            kakaoId = Long.parseLong(providerId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("providerId는 숫자 형식이어야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        user.setKakaoId(kakaoId);
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        System.out.println("====== 회원 탈퇴 디버깅 ======");
        System.out.println("User ID: " + user.getId());
        System.out.println("LoginType: " + user.getLoginType());
        System.out.println("KakaoId: " + user.getKakaoId());

        if (user.getLoginType() == User.LoginType.KAKAO && user.getKakaoId() != null) {
            System.out.println("-> 카카오 연동 해제(Unlink) 호출 시작!");
            kakaoOAuthService.unlinkKakaoUser(user.getKakaoId());
        } else {
            System.out.println("-> 조건 불일치로 카카오 연동 해제를 스킵합니다.");
        }

        // 기존 연관 데이터 삭제 로직
        commentRepository.deleteByUser_Id(userId);
        postBookmarkRepository.deleteByUser_Id(userId);
        postLikeRepository.deleteByUser_Id(userId);
        commentLikeRepository.deleteByPostOwnerId(userId);
        commentRepository.deleteByPost_User_Id(userId);
        postBookmarkRepository.deleteByPost_User_Id(userId);
        postLikeRepository.deleteByPost_User_Id(userId);
        postRepository.deleteByUser_Id(userId);
        academyRepository.deleteByUser_Id(userId);

        // 💡 학부모 탈퇴 시 연관된 자녀(Student) 데이터 삭제 추가!
        studentRepository.deleteByParentId(userId);

        // 최종 유저 삭제
        userRepository.delete(user);
    }

}