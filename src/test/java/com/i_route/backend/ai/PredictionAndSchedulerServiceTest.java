package com.i_route.backend.ai;

import com.i_route.backend.ai.dto.PredictedScoreDto;
import com.i_route.backend.ai.dto.ReviewTodayDto;
import com.i_route.backend.ai.entity.AiRecommendation;
import com.i_route.backend.ai.entity.GradeEntity;
import com.i_route.backend.ai.entity.ReviewNotification;
import com.i_route.backend.ai.entity.StudyLogEntity;
import com.i_route.backend.ai.repository.AiRecommendationRepository;
import com.i_route.backend.ai.repository.GradeEntityRepository;
import com.i_route.backend.ai.repository.ReviewNotificationRepository;
import com.i_route.backend.ai.repository.StudyLogRepository;
import com.i_route.backend.ai.service.PredictionService;
import com.i_route.backend.ai.service.ReviewSchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionAndSchedulerServiceTest {

    // ============================================================
    // PredictionService
    // ============================================================

    @InjectMocks
    private PredictionService predictionService;

    @Mock private GradeEntityRepository gradeEntityRepository;
    @Mock private StudyLogRepository studyLogRepository;

    private GradeEntity gradeEntity(Long studentId, Long subjectId, int score, LocalDate date) {
        return GradeEntity.builder()
                .studentId(studentId).subjectId(subjectId)
                .score(score).testDate(date).build();
    }

    @Test
    @DisplayName("예상 점수 - 데이터 없으면 기본값 반환 (50±10)")
    void predictNextScore_noData() {
        given(gradeEntityRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(1L, 1L))
                .willReturn(Collections.emptyList());

        PredictedScoreDto result = predictionService.predictNextScore(1L, 1L);

        assertThat(result.getExpectedScoreMin()).isEqualTo(40);
        assertThat(result.getExpectedScoreMax()).isEqualTo(60);
        assertThat(result.getAchievementProbability()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("예상 점수 - 데이터 1건이면 기본값 반환")
    void predictNextScore_oneDataPoint() {
        given(gradeEntityRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(1L, 1L))
                .willReturn(List.of(gradeEntity(1L, 1L, 70, LocalDate.now().minusMonths(1))));

        PredictedScoreDto result = predictionService.predictNextScore(1L, 1L);

        assertThat(result.getExpectedScoreMin()).isEqualTo(60);
        assertThat(result.getExpectedScoreMax()).isEqualTo(80);
    }

    @Test
    @DisplayName("예상 점수 - 상승 추세면 달성 확률 50% 이상")
    void predictNextScore_upwardTrend() {
        LocalDate base = LocalDate.now().minusMonths(3);
        given(gradeEntityRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(1L, 1L))
                .willReturn(List.of(
                        gradeEntity(1L, 1L, 60, base),
                        gradeEntity(1L, 1L, 70, base.plusMonths(1)),
                        gradeEntity(1L, 1L, 80, base.plusMonths(2))
                ));
        given(studyLogRepository.findByStudentId(1L)).willReturn(Collections.emptyList());

        PredictedScoreDto result = predictionService.predictNextScore(1L, 1L);

        assertThat(result.getAchievementProbability()).isGreaterThanOrEqualTo(50.0);
        assertThat(result.getExpectedScoreMin()).isGreaterThanOrEqualTo(0);
        assertThat(result.getExpectedScoreMax()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("예상 점수 - 학습 시간 보너스 적용 (일평균 60분 이상)")
    void predictNextScore_studyBonus() {
        LocalDate base = LocalDate.now().minusMonths(2);
        given(gradeEntityRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(1L, 1L))
                .willReturn(List.of(
                        gradeEntity(1L, 1L, 70, base),
                        gradeEntity(1L, 1L, 75, base.plusMonths(1))
                ));

        StudyLogEntity log1 = StudyLogEntity.builder().studentId(1L).durationMinutes(90).build();
        StudyLogEntity log2 = StudyLogEntity.builder().studentId(1L).durationMinutes(70).build();
        given(studyLogRepository.findByStudentId(1L)).willReturn(List.of(log1, log2));

        PredictedScoreDto result = predictionService.predictNextScore(1L, 1L);

        // 학습 보너스 10점 추가로 확률이 더 높아야 함
        assertThat(result.getAchievementProbability()).isGreaterThan(55.0);
    }

    @Test
    @DisplayName("예상 점수 - 항상 0~100 범위 내")
    void predictNextScore_withinBounds() {
        LocalDate base = LocalDate.now().minusMonths(3);
        given(gradeEntityRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(1L, 1L))
                .willReturn(List.of(
                        gradeEntity(1L, 1L, 95, base),
                        gradeEntity(1L, 1L, 98, base.plusMonths(1)),
                        gradeEntity(1L, 1L, 99, base.plusMonths(2))
                ));
        given(studyLogRepository.findByStudentId(1L)).willReturn(Collections.emptyList());

        PredictedScoreDto result = predictionService.predictNextScore(1L, 1L);

        assertThat(result.getExpectedScoreMin()).isGreaterThanOrEqualTo(0);
        assertThat(result.getExpectedScoreMax()).isLessThanOrEqualTo(100);
    }

    // ============================================================
    // ReviewSchedulerService
    // ============================================================

    @InjectMocks
    private ReviewSchedulerService schedulerService;

    @Mock private AiRecommendationRepository aiRecommendationRepository;
    @Mock private ReviewNotificationRepository reviewNotificationRepository;

    private AiRecommendation rec(String studentId, String title) {
        return AiRecommendation.builder()
                .studentId(studentId).title(title).build();
    }

    @Test
    @DisplayName("오늘 복습 목록 - 1일/3일/7일 전 항목 합산 반환")
    void getTodayReviews_combined() {
        LocalDate today = LocalDate.now();

        given(aiRecommendationRepository.findByStudentIdAndCreatedAtBetween(
                eq("S-001"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of(rec("S-001", "이차방정식 풀이")))
                .willReturn(Collections.emptyList())
                .willReturn(List.of(rec("S-001", "연립방정식")));

        ReviewTodayDto result = schedulerService.getTodayReviews("S-001");

        assertThat(result.isHasReview()).isTrue();
        assertThat(result.getReviews()).hasSize(2);
    }

    @Test
    @DisplayName("오늘 복습 목록 - 복습 항목 없으면 hasReview false")
    void getTodayReviews_noItems() {
        given(aiRecommendationRepository.findByStudentIdAndCreatedAtBetween(
                anyString(), any(), any()))
                .willReturn(Collections.emptyList());

        ReviewTodayDto result = schedulerService.getTodayReviews("S-001");

        assertThat(result.isHasReview()).isFalse();
        assertThat(result.getReviews()).isEmpty();
    }

    @Test
    @DisplayName("복습 알림 즉시 생성 - 3개 날짜 합산 카운트 반환")
    void triggerReviewNotifications_countReturned() {
        given(aiRecommendationRepository.findByStudentIdAndCreatedAtBetween(
                eq("S-001"), any(), any()))
                .willReturn(List.of(rec("S-001", "A")))        // 1일 전
                .willReturn(List.of(rec("S-001", "B"), rec("S-001", "C"))) // 3일 전
                .willReturn(Collections.emptyList());           // 7일 전

        given(reviewNotificationRepository.save(any(ReviewNotification.class)))
                .willAnswer(inv -> inv.getArgument(0));

        int count = schedulerService.triggerReviewNotificationsForStudent("S-001");

        assertThat(count).isEqualTo(3);
        then(reviewNotificationRepository).should(times(3)).save(any(ReviewNotification.class));
    }

    @Test
    @DisplayName("복습 알림 즉시 생성 - 복습 항목 없으면 0 반환")
    void triggerReviewNotifications_zeroWhenEmpty() {
        given(aiRecommendationRepository.findByStudentIdAndCreatedAtBetween(
                anyString(), any(), any()))
                .willReturn(Collections.emptyList());

        int count = schedulerService.triggerReviewNotificationsForStudent("S-001");

        assertThat(count).isZero();
        then(reviewNotificationRepository).should(never()).save(any());
    }
}
