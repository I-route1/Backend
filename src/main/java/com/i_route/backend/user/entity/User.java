package com.i_route.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nickname;

    private String password; // 카카오 유저는 임시 비밀번호 혹은 null

    @Column(unique = true) // 이메일도 중복 방지를 위해 unique 추천
    private String email;

    // 1. 기존 String role을 안전한 Enum 형태로 변경!
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private Long kakaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    @Column(unique = true)
    private String resetToken;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailVerified = false;

    // ==========================================
    // Inner Enums (로그인 타입 & 유저 역할)
    // ==========================================
    public enum LoginType {
        EMAIL, KAKAO
    }

    // 학부모, 학원강사, 운전기사 역할 정의
    public enum UserRole {
        PARENT,    // 학부모
        TEACHER,   // 학원강사
        DRIVER,    // 운전기사
        ADMIN   // 운영자
    }

    // ==========================================
    // 비즈니스 메서드
    // ==========================================
    public void updateResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public void changePassword(String encryptedPassword) {
        this.password = encryptedPassword;
    }

    public static User ofKakao(Long kakaoId, String nickname, String email) {
        return User.builder()
                .kakaoId(kakaoId)
                .nickname(nickname)
                .email(email != null ? email : "kakao_" + kakaoId + "@kakao.com")
                .password(UUID.randomUUID().toString())
                .role(UserRole.PARENT) // 카카오 최초 가입 시 기본 역할 지정 (예: 학부모)
                .loginType(LoginType.KAKAO)
                .build();
    }
}
