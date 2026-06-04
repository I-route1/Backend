package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.ReviewTodayDto;
import com.i_route.backend.ai.entity.AiRecommendation;
import com.i_route.backend.ai.entity.ReviewNotification;
import com.i_route.backend.ai.repository.AiRecommendationRepository;
import com.i_route.backend.ai.repository.ReviewNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSchedulerService {

    private final AiRecommendationRepository aiRecommendationRepository;
    private final ReviewNotificationRepository reviewNotificationRepository;

    // 매일 아침 9시 정각에 자동 실행
    @Scheduled(cron = "0 0 9 * * *")
    public void sendEbbinghausReviewAlerts() {
        log.info("⏰ 에빙하우스 망각 곡선 복습 알림 스케줄러 가동 시작!");

        LocalDate today = LocalDate.now();

        // 1일 전, 3일 전, 7일 전 날짜 세팅
        processReviewForDate(today.minusDays(1), "1일 차");
        processReviewForDate(today.minusDays(3), "3일 차");
        processReviewForDate(today.minusDays(7), "7일 차");

        log.info("✅ 오늘의 망각 곡선 복습 알림 전송 로직 완료!");
    }

    /**
     * 앱 실행 시 프론트엔드가 호출 — 오늘 복습해야 할 항목 반환
     */
    public ReviewTodayDto getTodayReviews(Long studentId) {
        LocalDate today = LocalDate.now();
        List<ReviewTodayDto.ReviewItem> items = new ArrayList<>();

        collectReviewItems(studentId, today.minusDays(1), "1일 차 복습", items);
        collectReviewItems(studentId, today.minusDays(3), "3일 차 복습", items);
        collectReviewItems(studentId, today.minusDays(7), "7일 차 복습", items);

        return ReviewTodayDto.builder()
                .studentId(studentId)
                .hasReview(!items.isEmpty())
                .reviews(items)
                .build();
    }

    private void collectReviewItems(Long studentId, LocalDate targetDate, String dayLabel,
                                    List<ReviewTodayDto.ReviewItem> items) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<AiRecommendation> found = aiRecommendationRepository
                .findByStudentIdAndCreatedAtBetween(studentId, start, end);

        for (AiRecommendation rec : found) {
            items.add(ReviewTodayDto.ReviewItem.builder()
                    .id(rec.getId())
                    .title(rec.getTitle())
                    .dayLabel(dayLabel)
                    .originalDate(targetDate.toString())
                    .message("복습할 내용이 있습니다: " + rec.getTitle())
                    .build());
        }
    }

    /**
     * 프론트엔드 요청 — 특정 학생의 에빙하우스 복습 알림을 즉시 생성
     */
    public int triggerReviewNotificationsForStudent(Long studentId) {
        LocalDate today = LocalDate.now();
        int count = 0;
        count += createNotificationsForStudent(studentId, today.minusDays(1), "1일 차");
        count += createNotificationsForStudent(studentId, today.minusDays(3), "3일 차");
        count += createNotificationsForStudent(studentId, today.minusDays(7), "7일 차");
        log.info("학생 {} 복습 알림 {}건 생성 완료", studentId, count);
        return count;
    }

    private int createNotificationsForStudent(Long studentId, LocalDate targetDate, String dayLabel) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<AiRecommendation> recs = aiRecommendationRepository
                .findByStudentIdAndCreatedAtBetween(studentId, start, end);

        for (AiRecommendation rec : recs) {
            String title = rec.getTitle() != null ? rec.getTitle() : "[" + dayLabel + "] 복습 알림";
            reviewNotificationRepository.save(ReviewNotification.builder()
                    .studentId(studentId)
                    .title(title)
                    .message(dayLabel + " 복습할 내용이 있습니다: " + title)
                    .build());
        }
        return recs.size();
    }

    public void deleteReview(Long reviewId) {
        if (!aiRecommendationRepository.existsById(reviewId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "복습 항목을 찾을 수 없습니다.");
        }
        aiRecommendationRepository.deleteById(reviewId);
    }

    /**
     * 특정 날짜에 저장된 AI 족보를 조회하여 복습 알림을 보내는 내부 메서드
     */
    private void processReviewForDate(LocalDate targetDate, String dayLabel) {
        // 해당 날짜의 00:00:00 부터 23:59:59 까지 설정
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        // DB에서 해당 날짜에 저장된 족보 데이터 싹 다 가져오기
        List<AiRecommendation> reviews = aiRecommendationRepository.findByCreatedAtBetween(startOfDay, endOfDay);

        if (reviews.isEmpty()) {
            return; // 복습할 내용이 없으면 패스
        }

        log.info("📅 [{} 복습] {} 날짜의 복습 대상자 {}명 발견!", dayLabel, targetDate, reviews.size());

        for (AiRecommendation review : reviews) {
            String title = review.getTitle() != null ? review.getTitle() : "[" + dayLabel + "] 복습 알림";
            String message = dayLabel + " 복습할 내용이 있습니다: " + title;

            ReviewNotification notification = ReviewNotification.builder()
                    .studentId(review.getStudentId())
                    .title(title)
                    .message(message)
                    .build();
            reviewNotificationRepository.save(notification);
            log.info("📢 인앱 복습 알림 저장 완료: 학생 ID = {}", review.getStudentId());
        }
    }
}