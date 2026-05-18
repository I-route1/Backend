package com.i_route.backend.domain.notification.repository;

import com.i_route.backend.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class NotificationDedupRedisRepository {

    private final StringRedisTemplate redisTemplate;

    public boolean canSend(
            NotificationType type,
            Long targetId
    ) {
        String key = "notification:" + type + ":" + targetId;

        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            return false;
        }

        redisTemplate.opsForValue()
                .set(key, "sent", Duration.ofMinutes(5));

        return true;
    }
}
