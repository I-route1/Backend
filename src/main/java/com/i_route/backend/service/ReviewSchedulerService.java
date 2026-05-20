package com.i_route.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReviewSchedulerService {

    // 매일 아침 9시 0분 0초에 자동으로 실행되는 메서드 (Cron 표현식)
    @Scheduled(cron = "0 0 9 * * *")
    public void sendEbbinghausReviewAlerts() {
        log.info("⏰ 에빙하우스 망각 곡선 복습 알림 스케줄러 가동 시작!");

        // TODO: 1. DB에서 오늘 복습 주기가 도래한 (1일차, 3일차, 7일차) 오답 노트 목록 조회
        // TODO: 2. 해당 학생들에게 푸시 알림(FCM) 또는 앱 내 알림 데이터 생성 및 전송

        log.info("✅ 오늘의 복습 알림 전송 완료!");
    }
}