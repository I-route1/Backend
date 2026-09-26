package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.entity.Grade;
import com.i_route.backend.ai.repository.GradeRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiReportController {

    /** AI 서버 생성이 길어져도 이 정도면 충분하다 (어댑터 생성 5~15초 + RAG 검색). */
    // CloudFront 원본 응답 한도(OriginReadTimeout 60초)보다 짧아야 한다. 같거나 길면 AI가 늦을 때
    // 규칙 기반 폴백을 만들기 전에 CloudFront가 504를 돌려준다. 개념 설명은 보통 20초, 최대 40초.
    private static final Duration AI_TIMEOUT = Duration.ofSeconds(50);

    private final GradeRepository gradeRepository;
    private final WebClient fastApiWebClient;

    public AiReportController(GradeRepository gradeRepository,
                              @Qualifier("fastApiWebClient") WebClient fastApiWebClient) {
        this.gradeRepository = gradeRepository;
        this.fastApiWebClient = fastApiWebClient;
    }

    /**
     * POST /api/ai/predict
     * Body: { past_score_avg, study_hours_per_day }
     * Response: { expected_score, message }
     */
    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predictScore(@RequestBody PredictRequest request) {
        double base = request.getPastScoreAvg();
        double hours = request.getStudyHoursPerDay();

        // 선형 예측: 하루 1시간 → +2.5점, 최대 100점
        double expected = Math.min(100.0, base + (hours * 2.5));
        int expectedScore = (int) Math.round(expected);

        String message;
        if (expectedScore >= 90)      message = "현재 학습량이라면 최상위권 진입이 가능합니다!";
        else if (expectedScore >= 80) message = "꾸준한 학습으로 상위권을 유지할 수 있습니다.";
        else if (expectedScore >= 70) message = "조금 더 노력하면 큰 성적 향상을 기대할 수 있습니다.";
        else                          message = "학습 시간을 늘리면 빠른 성적 향상이 가능합니다.";

        Map<String, Object> result = new HashMap<>();
        result.put("expected_score", expectedScore);
        result.put("message", message);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ai/report/subject-recommend?studentId={}&subject={}
     * Response: { studentId, subject, targetConcept, aiRecommendationReport }
     */
    @PostMapping("/report/subject-recommend")
    public ResponseEntity<Map<String, Object>> subjectRecommend(
            @RequestParam Long studentId,
            @RequestParam String subject) {

        List<Grade> grades = gradeRepository
                .findByStudentIdAndSubjectOrderByExamDateDesc(studentId, subject);

        // 취약 개념 태그 추출 (가장 최근 성적 기준)
        String targetConcept = grades.stream()
                .filter(g -> g.getWeakConceptTag() != null && !g.getWeakConceptTag().isBlank())
                .map(Grade::getWeakConceptTag)
                .findFirst()
                .orElse(subject + " 기본 개념");

        // 평균 점수 계산
        double avgScore = grades.stream()
                .mapToInt(Grade::getScore)
                .average()
                .orElse(0.0);

        // 점수 추세 파악 (최근 2회)
        String trend = "유지";
        if (grades.size() >= 2) {
            int gap = grades.get(0).getScore() - grades.get(1).getScore();
            if (gap > 0) trend = "상승";
            else if (gap < 0) trend = "하락";
        }

        // 1순위: AI 서버에 위임. 과목별 파인튜닝 어댑터 + RAG 검색을 쓰므로
        // 아래 규칙 기반 템플릿보다 내용이 구체적이다.
        Map<String, Object> aiResult = requestAiRecommendation(studentId, subject, targetConcept);
        if (aiResult != null) {
            return ResponseEntity.ok(aiResult);
        }

        // 2순위: AI 서버가 꺼져 있거나 실패하면 규칙 기반으로 폴백한다.
        // 응답 형식이 같아서 호출하는 쪽은 차이를 알 필요가 없다.
        String report = buildRecommendationReport(subject, targetConcept, avgScore, trend);

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("subject", subject);
        result.put("targetConcept", targetConcept);
        result.put("aiRecommendationReport", report);
        return ResponseEntity.ok(result);
    }

    /** AI 서버 호출. 실패하면 null을 반환해 호출부가 규칙 기반으로 폴백하게 한다. */
    private Map<String, Object> requestAiRecommendation(Long studentId, String subject, String conceptTag) {
        try {
            Map<String, Object> body = fastApiWebClient.post()
                    // FastAPI 쪽 파라미터명이 snake_case다 (main.py의 Query 선언).
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/ai/report/subject-recommend")
                            .queryParam("student_id", studentId)
                            .queryParam("subject", subject)
                            .queryParam("concept_tag", conceptTag)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(AI_TIMEOUT);

            if (body != null && body.get("aiRecommendationReport") != null) {
                return body;
            }
            log.warn("[AI 추천] 응답에 aiRecommendationReport가 없어 규칙 기반으로 폴백합니다: {}", body);
        } catch (Exception e) {
            log.warn("[AI 추천] AI 서버 호출 실패 — 규칙 기반으로 폴백합니다: {}", e.getMessage());
        }
        return null;
    }

    private String buildRecommendationReport(String subject, String concept, double avg, String trend) {
        String levelDesc;
        String strategy;

        if (avg >= 85) {
            levelDesc = "상위권";
            strategy = "심화 문제와 실전 모의고사 위주로 학습하여 실수를 줄이세요.";
        } else if (avg >= 70) {
            levelDesc = "중위권";
            strategy = "개념 정리와 유형별 반복 학습을 병행하세요.";
        } else {
            levelDesc = "기초 다지기 단계";
            strategy = "핵심 개념부터 탄탄하게 정리하고 기본 문제를 반복 풀이하세요.";
        }

        String trendComment = switch (trend) {
            case "상승" -> "최근 성적이 오르고 있습니다. 지금 방식을 유지하세요.";
            case "하락" -> "최근 성적이 하락하고 있습니다. 학습 방법 점검이 필요합니다.";
            default -> "성적이 안정적으로 유지되고 있습니다.";
        };

        String studySteps = switch (subject) {
            case "수학" -> "1. 공식·정리 정확히 암기 후 유도과정 이해\n" +
                          "2. 개념 적용 기본 문제 10문항 풀이\n" +
                          "3. 틀린 문제 오답 노트 — 실수 유형 분류\n" +
                          "4. 심화·응용 문제로 마무리";
            case "영어" -> "1. 핵심 문법 규칙 + 예문으로 패턴 암기\n" +
                          "2. 단문 → 장문 순서로 독해 적용 연습\n" +
                          "3. 오답 어휘·표현 단어장 정리\n" +
                          "4. 실전 모의고사 시간 재며 풀기";
            case "국어" -> "1. 개념어·문학 용어·갈래 특성 정리\n" +
                          "2. 기출 지문 분석 — 화자·서술자 파악\n" +
                          "3. 선지 오답 이유 직접 설명하기\n" +
                          "4. 실전 기출 모의고사 풀이";
            case "사회", "한국사" -> "1. 시대·주제별 흐름 연표 정리\n" +
                                    "2. 핵심 개념 키워드 암기\n" +
                                    "3. 자료·사진 분석 문제 집중 연습\n" +
                                    "4. 기출 선택지 오답 분석";
            case "과학" -> "1. 원리·법칙 개념 정리 + 실험 과정 이해\n" +
                          "2. 개념 적용 계산 문제 풀이\n" +
                          "3. 그래프·표 해석 유형 집중 연습\n" +
                          "4. 통합과학 연계 문제 풀기";
            default -> "1. 핵심 개념 정리\n" +
                       "2. 취약 개념 기출문제 10문항 풀이\n" +
                       "3. 오답 노트 작성 및 유사 문제 반복\n" +
                       "4. 실전 문제로 마무리";
        };

        return String.format(
                "[%s] %s 취약 개념 분석 리포트\n\n" +
                "현재 수준: %s (평균 %.0f점)\n" +
                "추세: %s\n\n" +
                "취약 개념: %s\n\n" +
                "AI 추천 학습 전략: %s\n\n" +
                "추천 학습 순서:\n%s",
                subject, concept, levelDesc, avg, trendComment, concept, strategy, studySteps
        );
    }

    @Data
    public static class PredictRequest {
        private double past_score_avg;
        private double study_hours_per_day;

        public double getPastScoreAvg() { return past_score_avg; }
        public double getStudyHoursPerDay() { return study_hours_per_day; }
    }
}
