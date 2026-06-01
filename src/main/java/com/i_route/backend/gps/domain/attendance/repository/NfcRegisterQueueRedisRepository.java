package com.i_route.backend.gps.domain.attendance.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NfcRegisterQueueRedisRepository {

    private static final String KEY = "nfc:register:queue";
    private final StringRedisTemplate redisTemplate;

    public void enqueue(Long studentId) {
        redisTemplate.opsForList().leftPush(KEY, studentId.toString());
    }

    public Optional<Long> dequeue() {
        String value = redisTemplate.opsForList().rightPop(KEY);
        return Optional.ofNullable(value).map(Long::parseLong);
    }
}
