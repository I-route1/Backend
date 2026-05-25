package com.i_route.backend.gps.domain.gps.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.gps.domain.gps.dto.response.CurrentBusLocationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CurrentLocationRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(CurrentBusLocationResponse response) {
        try {
            String key = key(response.getBusId());
            String value = objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue()
                    .set(key, value, Duration.ofMinutes(10));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("현재 위치 Redis 저장 실패");
        }
    }

    public Optional<CurrentBusLocationResponse> findByBusId(Long busId) {
        try {
            String value = redisTemplate.opsForValue().get(key(busId));

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(
                    objectMapper.readValue(
                            value,
                            CurrentBusLocationResponse.class
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException("현재 위치 Redis 조회 실패");
        }
    }

    public void delete(Long busId) {
        redisTemplate.delete(key(busId));
    }

    private String key(Long busId) {
        return "bus:" + busId + ":current-location";
    }
}
