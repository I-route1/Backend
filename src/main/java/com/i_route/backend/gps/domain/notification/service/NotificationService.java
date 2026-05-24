package com.i_route.backend.gps.domain.notification.service;

import com.i_route.backend.gps.domain.notification.entity.NotificationType;
import com.i_route.backend.gps.domain.notification.repository.NotificationDedupRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationDedupRedisRepository dedupRepository;

    public void sendBoarding(Long studentId) {
        if (!dedupRepository.canSend(NotificationType.BOARDING, studentId)) {
            return;
        }

        System.out.println("[FCM] 학생 탑승 알림 studentId=" + studentId);
    }

    public void sendDropOff(Long studentId) {
        if (!dedupRepository.canSend(NotificationType.DROPOFF, studentId)) {
            return;
        }

        System.out.println("[FCM] 학생 하차 알림 studentId=" + studentId);
    }

    public void sendLateRisk(
            Long studentId,
            Integer delayMinutes
    ) {
        if (!dedupRepository.canSend(NotificationType.LATE_RISK, studentId)) {
            return;
        }

        System.out.println(
                "[FCM] 지각 위험 알림 studentId="
                        + studentId
                        + ", delay="
                        + delayMinutes
        );
    }

    public void sendAbnormalStop(Long busId) {
        if (!dedupRepository.canSend(NotificationType.ABNORMAL_STOP, busId)) {
            return;
        }

        System.out.println("[FCM] 비정상 정차 알림 busId=" + busId);
    }

    public void sendRouteDeviation(Long busId) {
        if (!dedupRepository.canSend(NotificationType.ROUTE_DEVIATION, busId)) {
            return;
        }

        System.out.println("[FCM] 경로 이탈 알림 busId=" + busId);
    }
}
