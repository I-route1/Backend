package com.i_route.backend.ai;

import com.i_route.backend.ai.dto.MaterialRecommendationDto;
import com.i_route.backend.ai.dto.PeerSuccessPathDto;
import com.i_route.backend.ai.dto.StudyMethodDto;
import com.i_route.backend.ai.dto.StudyRoadmapDto;
import com.i_route.backend.ai.entity.*;
import com.i_route.backend.ai.repository.*;
import com.i_route.backend.ai.service.AiRecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {

    @InjectMocks
    private AiRecommendationService service;

    @Mock private StudentEntityRepository studentRepository;
    @Mock private StudyMaterialRepository materialRepository;
    @Mock private WrongAnswerEntityRepository wrongAnswerRepository;
    @Mock private TargetGoalRepository targetGoalRepository;
    @Mock private GradeRepository gradeRepository;

    private StudentEntity student(Long id, int level, StudyTendency tendency) {
        return StudentEntity.builder().studentId(id).name("테스트").currentLevel(level)
                .learningTendency(tendency).build();
    }

    // ============================================================
    // 학습 자료 추천
    // ============================================================

    @Test
    @DisplayName("자료 추천 - 학생 레벨에 맞는 자료 반환")
    void recommendMaterials_success() {
        given(studentRepository.findById(1L)).willReturn(Optional.of(student(1L, 3, StudyTendency.VISUAL)));
        StudyMaterial mat = StudyMaterial.builder().materialId(10L).title("기초 수학").materialType("PDF").level(3).build();
        given(materialRepository.findByLevelLessThanEqualOrderByLevelDesc(3)).willReturn(List.of(mat));

        List<MaterialRecommendationDto> result = service.recommendMaterials(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("기초 수학");
        assertThat(result.get(0).getMatchReason()).contains("3");
    }

    @Test
    @DisplayName("자료 추천 - 학생 없으면 예외")
    void recommendMaterials_studentNotFound() {
        given(studentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommendMaterials(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("학생을 찾을 수 없습니다");
    }

    // ============================================================
    // 학습 방법 추천
    // ============================================================

    @Test
    @DisplayName("학습 방법 - 시각형 학생")
    void suggestStudyMethod_visual() {
        given(studentRepository.findById(1L)).willReturn(Optional.of(student(1L, 3, StudyTendency.VISUAL)));

        StudyMethodDto result = service.suggestStudyMethod(1L, 100L);

        assertThat(result.getTendency()).isEqualTo("VISUAL");
        assertThat(result.getStudyGuide()).contains("마인드맵");
    }

    @Test
    @DisplayName("학습 방법 - 청각형 학생")
    void suggestStudyMethod_auditory() {
        given(studentRepository.findById(2L)).willReturn(Optional.of(student(2L, 3, StudyTendency.AUDITORY)));

        StudyMethodDto result = service.suggestStudyMethod(2L, 100L);

        assertThat(result.getTendency()).isEqualTo("AUDITORY");
        assertThat(result.getStudyGuide()).contains("강의");
    }

    @Test
    @DisplayName("학습 방법 - 운동감각형 학생")
    void suggestStudyMethod_kinesthetic() {
        given(studentRepository.findById(3L)).willReturn(Optional.of(student(3L, 3, StudyTendency.KINESTHETIC)));

        StudyMethodDto result = service.suggestStudyMethod(3L, 100L);

        assertThat(result.getStudyGuide()).contains("문제 풀이");
    }

    // ============================================================
    // 목표 로드맵 추천
    // ============================================================

    @Test
    @DisplayName("목표 로드맵 - 정상 반환")
    void recommendGoalRoadmap_success() {
        TargetGoalEntity goal = TargetGoalEntity.builder()
                .studentId(1L).targetKeyword("수능 수학 1등급")
                .targetDate(LocalDate.now().plusWeeks(8))
                .dailyStudyHours(3).build();
        given(targetGoalRepository.findByStudentId(1L)).willReturn(Optional.of(goal));

        StudyRoadmapDto result = service.recommendGoalRoadmap(1L);

        assertThat(result.getTargetKeyword()).isEqualTo("수능 수학 1등급");
        assertThat(result.getWeeklyMilestones()).isNotEmpty();
        assertThat(result.getOverallStrategy()).contains("3시간");
    }

    @Test
    @DisplayName("목표 로드맵 - 목표 없으면 예외")
    void recommendGoalRoadmap_noGoal() {
        given(targetGoalRepository.findByStudentId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommendGoalRoadmap(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("목표가 없습니다");
    }

    // ============================================================
    // 에빙하우스 일일 복습 추천
    // ============================================================

    @Test
    @DisplayName("일일 복습 - 1일/3일/7일 전 오답 필터링")
    void recommendDailyReview_ebbinghausFilter() {
        LocalDate today = LocalDate.now();

        WrongAnswerEntity w1 = WrongAnswerEntity.builder()
                .studentId(1L).questionId(10L).failCount(2)
                .lastFailedDate(today.minusDays(1)).build();
        WrongAnswerEntity w2 = WrongAnswerEntity.builder()
                .studentId(1L).questionId(11L).failCount(1)
                .lastFailedDate(today.minusDays(3)).build();
        WrongAnswerEntity w3 = WrongAnswerEntity.builder()
                .studentId(1L).questionId(12L).failCount(1)
                .lastFailedDate(today.minusDays(5)).build(); // 필터 제외
        WrongAnswerEntity w4 = WrongAnswerEntity.builder()
                .studentId(1L).questionId(13L).failCount(3)
                .lastFailedDate(today.minusDays(7)).build();

        given(wrongAnswerRepository.findByStudentIdOrderByFailCountDesc(1L))
                .willReturn(List.of(w4, w1, w2, w3));

        List<WrongAnswerEntity> result = service.recommendDailyReview(1L);

        assertThat(result).hasSize(3);
        assertThat(result).doesNotContain(w3);
    }

    @Test
    @DisplayName("일일 복습 - 복습 대상 없으면 빈 리스트")
    void recommendDailyReview_empty() {
        WrongAnswerEntity w = WrongAnswerEntity.builder()
                .studentId(1L).questionId(10L).failCount(1)
                .lastFailedDate(LocalDate.now().minusDays(10)).build();

        given(wrongAnswerRepository.findByStudentIdOrderByFailCountDesc(1L))
                .willReturn(List.of(w));

        List<WrongAnswerEntity> result = service.recommendDailyReview(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("일일 복습 - lastFailedDate 없으면 제외")
    void recommendDailyReview_nullDateExcluded() {
        WrongAnswerEntity w = WrongAnswerEntity.builder()
                .studentId(1L).questionId(10L).failCount(1)
                .lastFailedDate(null).build();

        given(wrongAnswerRepository.findByStudentIdOrderByFailCountDesc(1L))
                .willReturn(List.of(w));

        List<WrongAnswerEntity> result = service.recommendDailyReview(1L);

        assertThat(result).isEmpty();
    }

    // ============================================================
    // 피어 콘텐츠 추천
    // ============================================================

    @Test
    @DisplayName("피어 콘텐츠 추천 - 성적 데이터 없으면 빈 리스트")
    void recommendByPeerContent_noGrade() {
        given(gradeRepository.getAverageScoreByStudent("S-001")).willReturn(null);

        List<MaterialRecommendationDto> result = service.recommendByPeerContent("S-001");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("피어 콘텐츠 추천 - 유사 성적대 자료 반환")
    void recommendByPeerContent_withGrade() {
        given(gradeRepository.getAverageScoreByStudent("S-001")).willReturn(80.0);
        given(gradeRepository.findAllStudentAverageScores())
                .willReturn(List.of(new Object[]{"S-002", 78.0}, new Object[]{"S-003", 50.0}));
        StudyMaterial mat = StudyMaterial.builder().materialId(1L).title("심화 문제집").materialType("BOOK").level(4).build();
        given(materialRepository.findByLevelLessThanEqualOrderByLevelDesc(4)).willReturn(List.of(mat));

        List<MaterialRecommendationDto> result = service.recommendByPeerContent("S-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMatchReason()).contains("1명"); // S-002만 ±10 이내
    }

    // ============================================================
    // 피어 성공 경로 추천
    // ============================================================

    @Test
    @DisplayName("피어 성공 경로 - 성적 데이터 없으면 메시지 반환")
    void recommendPeerSuccessPath_noData() {
        given(gradeRepository.findByStudentIdAndSubjectOrderByExamDateDesc("S-001", "수학"))
                .willReturn(Collections.emptyList());

        PeerSuccessPathDto result = service.recommendPeerSuccessPath("S-001", "수학");

        assertThat(result.getSimilarStudentsCount()).isZero();
        assertThat(result.getSuccessMessage()).contains("성적 데이터가 없어");
    }
}
