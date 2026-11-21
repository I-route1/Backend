package com.i_route.backend.gps.domain.notification.service;

import com.i_route.backend.gps.domain.notification.entity.NotificationType;
import com.i_route.backend.gps.domain.notification.repository.NotificationDedupRedisRepository;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationDedupRedisRepository dedupRepository;
    private final StudentRepository studentRepository;
    private final FirebasePushService firebasePushService;

    public void sendBoarding(Long studentId) {
        System.out.println("탑승 알림 studentId=" + studentId);
    }

    public void sendDropOff(Long studentId) {
        System.out.println("하차 알림 studentId=" + studentId);
    }

    public void sendLateRisk(Long studentId, Integer delayMinutes) {

        Student student = studentRepository.findById(studentId)
                .orElse(null);

        if (student == null || student.getParentId() == null) {
            return;
        }

        if (!dedupRepository.canSend(
                NotificationType.LATE_RISK, studentId)) {
            return;
        }

        firebasePushService.sendToUser(
                student.getParentId(),
                "통학 차량 지각 위험",
                student.getName() + " 학생의 차량이 약 "
                        + delayMinutes + "분 지연될 예정입니다."
        );
    }

    public void sendAbnormalStop(Long busId) {
        System.out.println("비정상 정차 알림 busId=" + busId);
    }

    public void sendRouteDeviation(Long busId) {

        var parentIds = studentRepository.findByBusId(busId)
                .stream()
                .map(Student::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (parentIds.isEmpty()) {
            return;
        }

        if (!dedupRepository.canSend(
                NotificationType.ROUTE_DEVIATION, busId)) {
            return;
        }

        for (Long parentId : parentIds) {
            firebasePushService.sendToUser(
                    parentId,
                    "통학 차량 경로 이탈",
                    "통학 차량이 지정된 경로를 이탈했습니다."
            );
        }
    }
}