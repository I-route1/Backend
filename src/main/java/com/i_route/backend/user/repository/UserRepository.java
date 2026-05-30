package com.i_route.backend.user.repository;

import com.i_route.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String usernmae);

    Optional<User> findByKakaoId(Long kakaoId);

    Optional<User> findByPhoneNumber(String PhoneNumber);

    Optional<User> findByNickname(String Nickname);

    Optional<User> findByEmailOrNicknameOrPhoneNumber(String email, String nickname, String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String cleanedPhoneNumber);

    boolean existsByNickname(String value);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByUsername(String username);
}

