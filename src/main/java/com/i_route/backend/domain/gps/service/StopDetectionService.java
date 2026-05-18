package com.i_route.backend.domain.gps.service;

import com.i_route.backend.domain.gps.dto.request.GpsLocationRequest;
import com.i_route.backend.domain.gps.entity.GpsEvent;
import com.i_route.backend.domain.gps.entity.GpsEventType;
import com.i_route.backend.domain.gps.repository.GpsEventRepository;
import com.i_route.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StopDetectionService {

    private static final double STOP_SPEED_THRESHOLD = 3.0;

    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;
    private final GpsEventRepository gpsEventRepository;

    public void detect(GpsLocationRequest request) {
        Long busId = request.getBusId();

        if (request.getSpeed() > STOP_SPEED_THRESHOLD) {
            redisTemplate.delete(stopKey(busId));
            return;
        }

        Boolean exists = redisTemplate.hasKey(stopKey(busId));

        if (Boolean.FALSE.equals(exists)) {
            redisTemplate.opsForValue()
                    .set(
                            stopKey(busId),
                            LocalDateTime.now().toString(),
                            Duration.ofMinutes(3)
                    );
            return;
        }

        gpsEventRepository.save(
                GpsEvent.builder()
                        .busId(busId)
                        .eventType(GpsEventType.ABNORMAL_STOP)
                        .message("차량이 3분 이상 정차했습니다.")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        notificationService.sendAbnormalStop(busId);
    }

    private String stopKey(Long busId) {
        return "bus:" + busId + ":stop-detection";
    }
}