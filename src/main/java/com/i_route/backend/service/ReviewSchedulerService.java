package com.i_route.backend.service;

import com.i_route.backend.entity.AiRecommendation;
import com.i_route.backend.repository.AiRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSchedulerService {

    private final AiRecommendationRepository aiRecommendationRepository;

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
            // TODO: 실제 프론트엔드(앱)로 푸시 알림(FCM 등)을 전송하는 로직 연결
            log.info(" 🔔 알림 발송 -> 학생 ID: {}, 복습 내용 요약: {}...",
                    review.getStudentId(),
                    review.getRecommendedContext().substring(0, Math.min(review.getRecommendedContext().length(), 20)));
        }
    }
}