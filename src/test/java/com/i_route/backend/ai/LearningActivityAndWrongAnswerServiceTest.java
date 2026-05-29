package com.i_route.backend.ai;

import com.i_route.backend.ai.dto.FeedbackRequest;
import com.i_route.backend.ai.dto.LearningActivityRequest;
import com.i_route.backend.ai.dto.LearningActivityResponse;
import com.i_route.backend.ai.entity.ErrorType;
import com.i_route.backend.ai.entity.LearningActivity;
import com.i_route.backend.ai.entity.WrongAnswer;
import com.i_route.backend.ai.repository.LearningActivityRepository;
import com.i_route.backend.ai.repository.WrongAnswerRepository;
import com.i_route.backend.ai.service.LearningActivityService;
import com.i_route.backend.ai.service.WrongAnswerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LearningActivityAndWrongAnswerServiceTest {

    // ============================================================
    // LearningActivityService
    // ============================================================

    @InjectMocks
    private LearningActivityService activityService;

    @Mock
    private LearningActivityRepository activityRepository;

    private LearningActivity activity(Long id, String studentId, String feedback) {
        return LearningActivity.builder()
                .id(id).studentId(studentId).subject("수학")
                .studyDate(LocalDate.now()).studyDurationMinutes(60)
                .understandingScore(4).concentrationScore(3)
                .instructorFeedback(feedback).build();
    }

    @Test
    @DisplayName("학습 기록 저장 성공")
    void saveActivity_success() {
        LearningActivityRequest req = new LearningActivityRequest();
        req.setStudentId("S-001"); req.setSubject("수학");
        req.setStudyDate(LocalDate.now()); req.setStudyDurationMinutes(90);
        req.setUnderstandingScore(5); req.setConcentrationScore(4);

        LearningActivity saved = activity(1L, "S-001", null);
        given(activityRepository.save(any(LearningActivity.class))).willReturn(saved);

        LearningActivityResponse resp = activityService.saveActivity(req);

        assertThat(resp.getSubject()).isEqualTo("수학");
    }

    @Test
    @DisplayName("학습 기록 조회 - 최신순 반환")
    void getActivitiesByStudent_returnsData() {
        given(activityRepository.findByStudentIdOrderByStudyDateDesc("S-001"))
                .willReturn(List.of(activity(1L, "S-001", null), activity(2L, "S-001", null)));

        List<LearningActivityResponse> result = activityService.getActivitiesByStudent("S-001");

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("강사 피드백 수정 성공")
    void updateFeedback_success() {
        LearningActivity saved = activity(1L, "S-001", "기존 피드백");
        given(activityRepository.findById(1L)).willReturn(Optional.of(saved));

        FeedbackRequest req = new FeedbackRequest();
        ReflectionTestUtils.setField(req, "instructorFeedback", "개선된 피드백");

        LearningActivityResponse resp = activityService.updateFeedback(1L, req);

        assertThat(resp.getInstructorFeedback()).isEqualTo("개선된 피드백");
    }

    @Test
    @DisplayName("강사 피드백 수정 실패 - 기록 없음")
    void updateFeedback_notFound() {
        given(activityRepository.findById(99L)).willReturn(Optional.empty());

        FeedbackRequest req = new FeedbackRequest();
        ReflectionTestUtils.setField(req, "instructorFeedback", "피드백");

        assertThatThrownBy(() -> activityService.updateFeedback(99L, req))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ============================================================
    // WrongAnswerService
    // ============================================================

    @InjectMocks
    private WrongAnswerService wrongAnswerService;

    @Mock
    private WrongAnswerRepository wrongAnswerRepository;

    private WrongAnswer wrongAnswer(String studentId, String questionId, int failCount) {
        WrongAnswer w = WrongAnswer.builder()
                .studentId(studentId).subject("수학").questionId(questionId)
                .conceptTag("이차방정식").errorType(ErrorType.CONCEPT_GAP)
                .failCount(failCount).build();
        return w;
    }

    @Test
    @DisplayName("오답 기록 - 신규 오답 저장")
    void recordWrongAnswer_newEntry() {
        given(wrongAnswerRepository.findByStudentIdAndQuestionId("S-001", "Q-001"))
                .willReturn(Optional.empty());

        WrongAnswer newWrong = wrongAnswer("S-001", "Q-001", 1);
        given(wrongAnswerRepository.save(any(WrongAnswer.class))).willReturn(newWrong);

        WrongAnswer result = wrongAnswerService.recordWrongAnswer(
                "S-001", "수학", "Q-001", "이차방정식", ErrorType.CONCEPT_GAP);

        assertThat(result.getStudentId()).isEqualTo("S-001");
        then(wrongAnswerRepository).should().save(any(WrongAnswer.class));
    }

    @Test
    @DisplayName("오답 기록 - 기존 오답 failCount 증가")
    void recordWrongAnswer_incrementExisting() {
        WrongAnswer existing = wrongAnswer("S-001", "Q-001", 2);
        given(wrongAnswerRepository.findByStudentIdAndQuestionId("S-001", "Q-001"))
                .willReturn(Optional.of(existing));
        given(wrongAnswerRepository.save(existing)).willReturn(existing);

        wrongAnswerService.recordWrongAnswer("S-001", "수학", "Q-001", "이차방정식", null);

        assertThat(existing.getFailCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("오답 기록 - errorType 업데이트")
    void recordWrongAnswer_updateErrorType() {
        WrongAnswer existing = wrongAnswer("S-001", "Q-001", 1);
        existing.setErrorType(ErrorType.CONCEPT_GAP);
        given(wrongAnswerRepository.findByStudentIdAndQuestionId("S-001", "Q-001"))
                .willReturn(Optional.of(existing));
        given(wrongAnswerRepository.save(existing)).willReturn(existing);

        wrongAnswerService.recordWrongAnswer("S-001", "수학", "Q-001", "이차방정식", ErrorType.CALCULATION_ERROR);

        assertThat(existing.getErrorType()).isEqualTo(ErrorType.CALCULATION_ERROR);
    }

    @Test
    @DisplayName("AI 취약점 조회 - 학생/과목별 반환")
    void getAiTargetWeakness_success() {
        List<WrongAnswer> expected = List.of(
                wrongAnswer("S-001", "Q-001", 3),
                wrongAnswer("S-001", "Q-002", 2)
        );
        given(wrongAnswerRepository.findTopWeaknessByStudentIdAndSubject("S-001", "수학"))
                .willReturn(expected);

        List<WrongAnswer> result = wrongAnswerService.getAiTargetWeakness("S-001", "수학");

        assertThat(result).hasSize(2);
    }
}
