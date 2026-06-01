package com.i_route.backend.gps.domain.attendance.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudentBoardingRedisRepository {

    private static final String KEY_PREFIX = "student:boarding:";
    private final StringRedisTemplate redisTemplate;

    public void markBoarded(Long studentId, Long busId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + studentId, busId.toString());
    }

    public void markExited(Long studentId) {
        redisTemplate.delete(KEY_PREFIX + studentId);
    }

    public Optional<Long> getBusId(Long studentId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + studentId);
        return Optional.ofNullable(value).map(Long::parseLong);
    }

    public boolean isOnBoard(Long studentId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + studentId));
    }
}
