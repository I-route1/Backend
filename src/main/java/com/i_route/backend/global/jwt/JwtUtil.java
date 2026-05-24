package com.i_route.backend.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // Secret Key 생성
    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Access Token 생성
    public String generateToken(Long userId) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getKey())
                .compact();
    }

    // username/email 기반 토큰 생성
    public String createToken(String username) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getKey())
                .compact();
    }

    // Refresh Token 생성
    public String generateRefreshToken(Long userId) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getKey())
                .compact();
    }

    // Subject 추출
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    // userId 추출
    public Long getUserId(String token) {
        return Long.parseLong(getSubject(token));
    }

    // 토큰 검증
    public boolean validateToken(String token) {

        try {
            parseClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            throw new RuntimeException("토큰이 만료되었습니다.");

        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
    }

    // Claims 파싱
    private Claims parseClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}