package com.i_route.backend.domain.auth.repository;

import com.i_route.backend.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(Long userId); // 로그아웃 시 삭제

    void deleteByToken(String token);
}

