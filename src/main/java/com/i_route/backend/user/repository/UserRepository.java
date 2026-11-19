package com.i_route.backend.user.repository;

import com.i_route.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByKakaoId(Long kakaoId);

    Optional<User> findByPhoneNumber(String PhoneNumber);

    Optional<User> findByNickname(String Nickname);

    List<User> findTop20ByNicknameContainingIgnoreCaseAndIdNotOrderByNicknameAsc(String nickname, Long id);

    Optional<User> findByEmailOrUsernameOrNicknameOrPhoneNumber(String Email, String username, String nickname, String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String cleanedPhoneNumber);

    boolean existsByNickname(String value);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByUsername(String username);

}

