package com.i_route.backend.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

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
                .claim("tokenType", "access")
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
                .claim("tokenType", "access")
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
                .claim("tokenType", "refresh")
                .setId(UUID.randomUUID().toString())
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
            var claims = parseClaims(token);
            if ("refresh".equals(claims.get("tokenType"))) throw new IllegalArgumentException("Access Token이 필요합니다.");
            return true;

        } catch (ExpiredJwtException e) {
            throw new RuntimeException("토큰이 만료되었습니다.");

        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            var claims = parseClaims(token);
            // 기존에 DB에 저장된 소셜 로그인 토큰은 만료 전까지 갱신할 수 있다.
            return claims.get("tokenType") == null || "refresh".equals(claims.get("tokenType"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
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
