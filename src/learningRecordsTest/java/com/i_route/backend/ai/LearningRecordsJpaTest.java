package com.i_route.backend.ai;

import com.i_route.backend.ai.dto.*;
import com.i_route.backend.ai.entity.*;
import com.i_route.backend.ai.repository.*;
import com.i_route.backend.ai.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "logging.level.org.hibernate.SQL=OFF"})
@ContextConfiguration(classes = LearningRecordsJpaTest.Config.class)
class LearningRecordsJpaTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.i_route.backend")
    @EnableJpaRepositories("com.i_route.backend.ai.repository")
    @Import({OcrResultService.class, WrongAnswerService.class, LearningActivityService.class, StudentRecordLock.class,
            AiRecommendationService.class})
    static class Config {}

    @Autowired OcrResultService ocr;
    @Autowired WrongAnswerService wrongAnswers;
    @Autowired LearningActivityService activities;
    @Autowired AiRecommendationService recommendations;
    @Autowired GradeRepository grades;
    @Autowired EntityManager em;

    @BeforeEach
    void students() {
        em.createNativeQuery("insert into student(student_id, name) values (14, 'test'), (15, 'other')").executeUpdate();
        em.flush();
    }

    @Test
    void peerRecommendationsUseNumericStudentIdsFromRealJpaAggregateAndExcludeSelf() {
        em.persist(Grade.builder().studentId(14L).subject("수학").score(80).build());
        em.persist(Grade.builder().studentId(14L).subject("영어").score(82).build());
        em.persist(Grade.builder().studentId(15L).subject("수학").score(79).build());
        em.persist(StudyMaterial.builder().title("함수 연습").materialType("교재").level(4).subjectId(2L).build());
        em.flush();
        em.clear();
        assertThat(grades.findAllStudentAverageScores()).allSatisfy(row -> assertThat(row[0]).isInstanceOf(String.class));
        assertThat(recommendations.recommendByPeerContent(14L)).singleElement().satisfies(r -> {
            assertThat(r.getTitle()).isEqualTo("함수 연습");
            assertThat(r.getMatchReason()).contains("1명");
        });
    }

    private OcrResultRequest request(Long studentId, String submissionId, String subject, boolean correct) {
        return new OcrResultRequest(studentId, subject, submissionId, 1, correct ? 1 : 0,
                List.of(new OcrResultRequest.Question("Q-" + submissionId, "돼지", "되지", 0.5,
                        correct, "받침", ErrorType.MEMORIZATION_GAP)));
    }

    @Test
    void persistsKoreanTextAndCountsAndDoesNotDuplicateOnRetry() {
        var input = request(14L, "worksheet-1", "국어", false);
        var saved = ocr.save(input);
        em.flush();
        em.clear();
        var loaded = ocr.get(saved.id(), 14L);
        assertThat(loaded.totalQuestions()).isEqualTo(1);
        assertThat(loaded.correctCount()).isZero();
        assertThat(loaded.questions()).isEqualTo(input.questions());
        assertThat(loaded.gradedAt()).isNotNull();
        assertThat(ocr.save(input).id()).isEqualTo(saved.id());
        em.flush();
        em.clear();
        assertThat(ocr.getByStudent(14L, null)).hasSize(1);
        assertThat(wrongAnswers.getWrongAnswers(14L, null)).singleElement()
                .satisfies(w -> assertThat(w.getFailCount()).isEqualTo(1));
    }

    @Test
    void savesPerfectScoreAndFiltersStudentAndSubject() {
        ocr.save(request(14L, "korean", "국어", true));
        ocr.save(request(14L, "math", "수학", false));
        ocr.save(request(15L, "other", "국어", false));
        em.flush();
        em.clear();
        assertThat(ocr.getByStudent(14L, null)).hasSize(2);
        assertThat(ocr.getByStudent(14L, "국어")).singleElement()
                .satisfies(r -> assertThat(r.correctCount()).isEqualTo(1));
        assertThat(wrongAnswers.getWrongAnswers(14L, null)).hasSize(1);
        assertThat(wrongAnswers.getWrongAnswers(14L, "국어")).isEmpty();
        assertThat(ocr.getByStudent(999L, null)).isEmpty();
        assertThat(wrongAnswers.getWrongAnswers(999L, null)).isEmpty();
    }

    @Test
    void rejectsConflictingSubmissionAndMismatchedStudentDetail() {
        var saved = ocr.save(request(14L, "same", "국어", false));
        assertThatThrownBy(() -> ocr.save(request(14L, "same", "수학", false)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThatThrownBy(() -> ocr.get(saved.id(), 15L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void listsAllWrongAnswersAcrossSubjectsInStableNewestOrder() {
        wrongAnswers.recordWrongAnswer(14L, "국어", "K1", "문법", null);
        wrongAnswers.recordWrongAnswer(14L, "수학", "M1", "함수", null);
        wrongAnswers.recordWrongAnswer(15L, "국어", "K2", "문법", null);
        em.flush();
        em.clear();
        assertThat(wrongAnswers.getWrongAnswers(14L, null)).extracting(WrongAnswer::getQuestionId)
                .containsExactly("M1", "K1");
        assertThat(wrongAnswers.getWrongAnswers(14L, "국어")).extracting(WrongAnswer::getQuestionId)
                .containsExactly("K1");
    }

    private LearningActivityRequest activity() {
        var request = new LearningActivityRequest();
        request.setStudentId(14L);
        request.setSubject("수학");
        request.setStudyDate(LocalDate.of(2026, 11, 4));
        request.setStudyDurationMinutes(30);
        request.setUnderstandingScore(4);
        request.setConcentrationScore(3);
        return request;
    }

    @Test
    void roundTripsCameraDetailsAndSupportsOldManualRecords() {
        var camera = activity();
        camera.setMeasurementSource(MeasurementSource.CAMERA);
        camera.setFocusedRatio(0.625);
        camera.setDrowsyRatio(0.125);
        activities.saveActivity(camera);
        activities.saveActivity(activity());
        em.persist(LearningActivity.builder().studentId(14L).subject("영어")
                .studyDate(LocalDate.of(2026, 11, 3)).build());
        em.flush();
        em.clear();
        var rows = activities.getActivitiesByStudent(14L);
        assertThat(rows).hasSize(3);
        assertThat(rows).filteredOn(r -> r.getMeasurementSource() == MeasurementSource.CAMERA)
                .singleElement().satisfies(r -> {
                    assertThat(r.getFocusedRatio()).isEqualTo(0.625);
                    assertThat(r.getDrowsyRatio()).isEqualTo(0.125);
                    assertThat(r.getConcentrationScore()).isEqualTo(3);
                });
        assertThat(rows).filteredOn(r -> r.getMeasurementSource() == MeasurementSource.MANUAL)
                .hasSize(2).allSatisfy(r -> {
                    assertThat(r.getFocusedRatio()).isNull();
                    assertThat(r.getDrowsyRatio()).isNull();
                });
    }
}
