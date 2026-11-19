package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.entity.ReviewNotification;
import com.i_route.backend.ai.repository.ReviewNotificationRepository;
import com.i_route.backend.ai.service.ReviewSchedulerService;
import com.i_route.backend.ai.service.StudentRecordAccess;
import com.i_route.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ReviewNotificationRepository reviewNotificationRepository;
    private final ReviewSchedulerService reviewSchedulerService;
    private final StudentRecordAccess studentRecordAccess;

    /**
     * GET /api/notifications?studentId={id}
     * 학생의 전체 알림 목록 조회
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewNotification>> getNotifications(
            @RequestParam Long studentId, @AuthenticationPrincipal CustomUserDetails user) {
        var notifications = reviewNotificationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        return ResponseEntity.ok(visibleNotifications(notifications, user));
    }

    /**
     * GET /api/notifications/unread?studentId={id}
     * 읽지 않은 알림만 조회
     */
    @GetMapping("/unread")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewNotification>> getUnreadNotifications(
            @RequestParam Long studentId, @AuthenticationPrincipal CustomUserDetails user) {
        var notifications = reviewNotificationRepository.findByStudentIdAndIsReadFalseOrderByCreatedAtDesc(studentId);
        return ResponseEntity.ok(visibleNotifications(notifications, user));
    }

    private List<ReviewNotification> visibleNotifications(
            List<ReviewNotification> notifications, CustomUserDetails user) {
        return user == null ? notifications : notifications.stream()
                .filter(notification -> !notification.isDismissedBy(user.getId()))
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void dismiss(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        ReviewNotification notification = reviewNotificationRepository.findById(id).orElse(null);
        if (notification != null) {
            studentRecordAccess.requireStudent(notification.getStudentId());
            notification.dismissFor(user.getId());
        }
    }

    /**
     * PATCH /api/notifications/{id}/read
     * 알림 읽음 처리
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ReviewNotification> markAsRead(@PathVariable Long id) {
        ReviewNotification notification = reviewNotificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
        notification.markAsRead();
        return ResponseEntity.ok(reviewNotificationRepository.save(notification));
    }

    /**
     * POST /api/notifications/trigger?studentId={id}
     * 에빙하우스 복습 알림 즉시 생성 (프론트엔드 요청용)
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerNotifications(@RequestParam Long studentId) {
        int created = reviewSchedulerService.triggerReviewNotificationsForStudent(studentId);
        return ResponseEntity.ok(Map.of(
                "studentId", studentId,
                "created", created,
                "message", created > 0 ? created + "건의 복습 알림이 생성되었습니다." : "복습할 항목이 없습니다."
        ));
    }
}
