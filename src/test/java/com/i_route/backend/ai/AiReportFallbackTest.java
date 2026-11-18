package com.i_route.backend.ai;

import com.i_route.backend.ai.controller.AiReportController;
import com.i_route.backend.ai.entity.Grade;
import com.i_route.backend.ai.repository.GradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiReportFallbackTest {
    private final GradeRepository grades = mock(GradeRepository.class);
    private static final long STUDENT = 72L;
    private static final String SUBJECT = "영어";

    private Map<String, Object> recommend(List<Grade> rows, HttpStatus status, String json) {
        when(grades.findByStudentIdAndSubjectOrderByExamDateDesc(STUDENT, SUBJECT)).thenReturn(rows);
        WebClient client = WebClient.builder().baseUrl("http://ai.test")
                .exchangeFunction(req -> Mono.just(ClientResponse.create(status)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(json).build()))
                .build();
        return new AiReportController(grades, client).subjectRecommend(STUDENT, SUBJECT).getBody();
    }

    private String fallback(Grade... rows) {
        return (String) recommend(List.of(rows), HttpStatus.SERVICE_UNAVAILABLE, "{}")
                .get("aiRecommendationReport");
    }

    private Grade grade(int score, String concept, String date) {
        return Grade.builder().studentId(STUDENT).subject(SUBJECT).score(score)
                .weakConceptTag(concept).examDate(date == null ? null : LocalDate.parse(date)).build();
    }

    @Test void absentGradesDoNotInventScoresTrendOrWeakness() {
        var output = recommend(List.of(), HttpStatus.SERVICE_UNAVAILABLE, "{}");
        assertThat(output).containsEntry("targetConcept", "영어 기본 개념")
                .containsEntry("studentId", STUDENT).containsEntry("subject", SUBJECT);
        assertThat((String) output.get("aiRecommendationReport")).contains("등록된 시험 성적이 없습니다", "기본 복습")
                .doesNotContain("0점", "0.0점", "상위권", "유지되고", "상승", "하락");
    }

    @Test void actualZeroCountsAsARecordedScoreButCannotEstablishTrend() {
        assertThat(fallback(grade(0, "수동태", "2026-10-03")))
                .contains("1회 평균 0.0점", "첫 시험은 0점", "다음 시험부터", "수동태")
                .doesNotContain("유지되고", "성적이 없습니다");
    }

    @Test void reportsObservedDifferenceAndLatestNonemptyConcept() {
        Grade last = grade(84, " ", "2026-11-02");
        Grade previous = grade(75, "수동태", "2026-10-02");
        assertThat(fallback(last, previous)).contains("2회 평균 79.5점", "75점에서 최근 84점으로 9점 올랐습니다", "수동태");
        assertThat(fallback(grade(70, null, "2026-11-02"), previous)).contains("5점 내려갔습니다");
        assertThat(fallback(grade(75, null, "2026-11-02"), previous)).contains("75점으로 같습니다");
    }

    @Test void tiedOrMissingDatesDoNotAssignChronology() {
        Grade last = grade(84, "수동태", "2026-11-02");
        assertThat(fallback(last, grade(75, "", "2026-11-02"))).contains("순서를 구분하기 어렵");
        assertThat(fallback(last, grade(75, "", null))).contains("순서를 구분하기 어렵");
    }

    @Test void successfulPythonReportRetainsItsOriginalPayload() {
        Map<String, Object> output = recommend(List.of(grade(72, "수동태", "2026-11-02")), HttpStatus.OK,
                "{\"subject\":\"영어\",\"targetConcept\":\"수동태\",\"aiRecommendationReport\":\"문장의 주어를 찾으세요.\"}");
        assertThat(output).containsEntry("aiRecommendationReport", "문장의 주어를 찾으세요.")
                .containsEntry("targetConcept", "수동태").hasSize(3);
    }
}
