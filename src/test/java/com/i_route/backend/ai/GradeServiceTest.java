package com.i_route.backend.ai;

import com.i_route.backend.ai.dto.GradeAnalysisResponse;
import com.i_route.backend.ai.dto.GradeRequest;
import com.i_route.backend.ai.dto.GradeResponse;
import com.i_route.backend.ai.entity.Grade;
import com.i_route.backend.ai.repository.GradeRepository;
import com.i_route.backend.ai.service.GradeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @InjectMocks
    private GradeService gradeService;

    @Mock
    private GradeRepository gradeRepository;

    private Grade grade(String studentId, String subject, int score, LocalDate date) {
        return Grade.builder()
                .studentId(studentId).subject(subject)
                .score(score).gradeLevel(2).examType("중간고사").examDate(date)
                .build();
    }

    // ============================================================
    // 성적 저장
    // ============================================================

    private GradeRequest gradeRequest(String studentId, String subject, int score,
                                       int gradeLevel, String examType, LocalDate examDate) {
        GradeRequest req = new GradeRequest();
        ReflectionTestUtils.setField(req, "studentId", studentId);
        ReflectionTestUtils.setField(req, "subject", subject);
        ReflectionTestUtils.setField(req, "score", score);
        ReflectionTestUtils.setField(req, "gradeLevel", gradeLevel);
        ReflectionTestUtils.setField(req, "examType", examType);
        ReflectionTestUtils.setField(req, "examDate", examDate);
        return req;
    }

    @Test
    @DisplayName("성적 저장 성공")
    void saveGrade_success() {
        GradeRequest req = gradeRequest("S-001", "수학", 85, 2, "중간고사", LocalDate.of(2026, 4, 1));

        Grade saved = grade("S-001", "수학", 85, LocalDate.of(2026, 4, 1));
        given(gradeRepository.save(any(Grade.class))).willReturn(saved);

        GradeResponse resp = gradeService.saveGrade(req);

        assertThat(resp.getScore()).isEqualTo(85);
        assertThat(resp.getSubject()).isEqualTo("수학");
    }

    // ============================================================
    // 성적 조회
    // ============================================================

    @Test
    @DisplayName("성적 조회 - 결과 있음")
    void getGradesByStudent_hasData() {
        given(gradeRepository.findByStudentIdOrderByExamDateDesc("S-001"))
                .willReturn(List.of(
                        grade("S-001", "수학", 90, LocalDate.of(2026, 4, 1)),
                        grade("S-001", "수학", 80, LocalDate.of(2026, 3, 1))
                ));

        List<GradeResponse> result = gradeService.getGradesByStudent("S-001");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getScore()).isEqualTo(90);
    }

    @Test
    @DisplayName("성적 조회 - 결과 없음")
    void getGradesByStudent_empty() {
        given(gradeRepository.findByStudentIdOrderByExamDateDesc("S-999"))
                .willReturn(Collections.emptyList());

        List<GradeResponse> result = gradeService.getGradesByStudent("S-999");

        assertThat(result).isEmpty();
    }

    // ============================================================
    // 성적 분석
    // ============================================================

    @Test
    @DisplayName("성적 분석 - 데이터 없으면 빈 결과 반환")
    void analyzeStudentGrades_noData() {
        given(gradeRepository.findByStudentIdOrderByExamDateDesc("S-001"))
                .willReturn(Collections.emptyList());

        GradeAnalysisResponse resp = gradeService.analyzeStudentGrades("S-001");

        assertThat(resp.getGradeHistory()).isEmpty();
        assertThat(resp.getSummaryMessage()).contains("아직 등록된");
    }

    @Test
    @DisplayName("성적 분석 - 성적 상승 감지")
    void analyzeStudentGrades_scoreImproved() {
        given(gradeRepository.findByStudentIdOrderByExamDateDesc("S-001"))
                .willReturn(List.of(
                        grade("S-001", "수학", 90, LocalDate.of(2026, 4, 1)),
                        grade("S-001", "수학", 75, LocalDate.of(2026, 3, 1))
                ));

        GradeAnalysisResponse resp = gradeService.analyzeStudentGrades("S-001");

        assertThat(resp.getSubjectScoreChanges().get("수학")).isEqualTo(15);
        assertThat(resp.getSummaryMessage()).contains("상승");
    }

    @Test
    @DisplayName("성적 분석 - 성적 하락 감지")
    void analyzeStudentGrades_scoreDropped() {
        given(gradeRepository.findByStudentIdOrderByExamDateDesc("S-001"))
                .willReturn(List.of(
                        grade("S-001", "국어", 60, LocalDate.of(2026, 4, 1)),
                        grade("S-001", "국어", 80, LocalDate.of(2026, 3, 1))
                ));

        GradeAnalysisResponse resp = gradeService.analyzeStudentGrades("S-001");

        assertThat(resp.getSubjectScoreChanges().get("국어")).isEqualTo(-20);
        assertThat(resp.getSummaryMessage()).contains("하락");
    }

    @Test
    @DisplayName("성적 분석 - 첫 시험만 있으면 변동 0 처리")
    void analyzeStudentGrades_firstExamOnly() {
        given(gradeRepository.findByStudentIdOrderByExamDateDesc("S-001"))
                .willReturn(List.of(grade("S-001", "영어", 70, LocalDate.of(2026, 4, 1))));

        GradeAnalysisResponse resp = gradeService.analyzeStudentGrades("S-001");

        assertThat(resp.getSubjectScoreChanges().get("영어")).isEqualTo(0);
        assertThat(resp.getSummaryMessage()).contains("첫 시험");
    }

    @Test
    @DisplayName("성적 분석 - 그래프 데이터 날짜 오름차순 정렬")
    void analyzeStudentGrades_historyAscending() {
        given(gradeRepository.findByStudentIdOrderByExamDateDesc("S-001"))
                .willReturn(List.of(
                        grade("S-001", "수학", 90, LocalDate.of(2026, 4, 1)),
                        grade("S-001", "수학", 80, LocalDate.of(2026, 3, 1)),
                        grade("S-001", "수학", 70, LocalDate.of(2026, 2, 1))
                ));

        GradeAnalysisResponse resp = gradeService.analyzeStudentGrades("S-001");

        List<GradeResponse> history = resp.getGradeHistory();
        assertThat(history.get(0).getExamDate()).isBefore(history.get(1).getExamDate());
        assertThat(history.get(1).getExamDate()).isBefore(history.get(2).getExamDate());
    }
}
