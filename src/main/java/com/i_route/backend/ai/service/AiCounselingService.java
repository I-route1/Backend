package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.AiReportRequest;
import com.i_route.backend.ai.dto.AiReportResponse;
import com.i_route.backend.ai.entity.AiRecommendation;
import com.i_route.backend.ai.repository.AiRecommendationRepository;
import com.i_route.backend.ai.repository.LearningActivityRepository;
import com.i_route.backend.ai.repository.WrongAnswerRepository;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Slf4j
@Service
// ❌ @RequiredArgsConstructor 제거 (수동 생성자와의 충돌 방지)
public class AiCounselingService {

    // AI 서버 rag.py의 _SUBJECT_KEYWORDS와 동일한 과목 집합.
    // 수학/국어(writing)는 전용 엔드포인트가 따로 있어 /{subject}로는 받지 않는다.
    private static final Set<String> SUBJECT_REPORT_SUPPORTED =
            Set.of("영어", "과학", "사회", "한국사");

    private final WebClient fastApiWebClient;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final StudentRepository studentRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final WrongAnswerRepository wrongAnswerRepository;

    public AiCounselingService(@Qualifier("fastApiWebClient") WebClient webClient,
                               AiRecommendationRepository aiRecommendationRepository,
                               StudentRepository studentRepository,
                               LearningActivityRepository learningActivityRepository,
                               WrongAnswerRepository wrongAnswerRepository) {
        this.fastApiWebClient = webClient;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.studentRepository = studentRepository;
        this.learningActivityRepository = learningActivityRepository;
        this.wrongAnswerRepository = wrongAnswerRepository;
    }

    // 1️⃣ [수학 메타인지 모델 가동]
    public Mono<AiReportResponse> generateMathReport(Long studentId) {
        log.info("📐 [수학 AI 가동] 학생 ID: {}의 진짜 데이터를 DB에서 조회합니다...", studentId);
        return fetchRealStudentData(studentId, "수학")
                .flatMap(realRequest -> sendToPythonServer("/api/ai/report/math", realRequest, "수학 메타인지 분석 리포트"));
    }

    // 2️⃣ [진로 탐색 및 작문 모델 가동]
    public Mono<AiReportResponse> generateWritingReport(Long studentId) {
        log.info("✍️ [진로/작문 AI 가동] 학생 ID: {}의 진짜 데이터를 DB에서 조회합니다...", studentId);
        return fetchRealStudentData(studentId, "국어")
                .flatMap(realRequest -> sendToPythonServer("/api/ai/report/writing", realRequest, "인공지능 기반 진로 탐색 리포트"));
    }

    // 3️⃣ [프리미엄 통합 분석 리포트 가동]
    public Mono<AiReportResponse> generatePremiumReport(Long studentId) {
        log.info("🚀 [통합 AI 가동] 학생 ID: {}의 진짜 데이터를 DB에서 조회합니다...", studentId);
        // 프리미엄은 AI 서버가 주 취약 과목을 고르므로 취약 개념도 그쪽에서 찾는다.
        return fetchRealStudentData(studentId, null)
                .flatMap(realRequest -> sendToPythonServer("/api/ai/report/premium", realRequest, "i-Route 프리미엄 통합 리포트"));
    }

    // 4️⃣ [전 과목 메타인지 리포트 - 영어/과학/사회/한국사]
    public Mono<AiReportResponse> generateSubjectReport(Long studentId, String subject) {
        if (!SUBJECT_REPORT_SUPPORTED.contains(subject)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 과목입니다: " + subject + " (지원: " + SUBJECT_REPORT_SUPPORTED + ")"
            ));
        }
        log.info("📚 [{} AI 가동] 학생 ID: {}의 진짜 데이터를 DB에서 조회합니다...", subject, studentId);
        return fetchRealStudentData(studentId, subject)
                .flatMap(realRequest -> sendToPythonServer(
                        // 과목명이 한글이라 UriBuilder로 경로 변수를 넘겨 인코딩을 맡긴다.
                        uriBuilder -> uriBuilder.path("/api/ai/report/{subject}").build(subject),
                        "/api/ai/report/" + subject,
                        realRequest,
                        subject + " 메타인지 분석 리포트"
                ));
    }

    // 🔍 [리액티브 특화 방어막] DB 블로킹 조회 격리
    // weakSubject가 있으면 그 과목의 최다 오답 개념을 weakConcept로 함께 넘긴다.
    private Mono<AiReportRequest> fetchRealStudentData(Long studentId, String weakSubject) {
        return Mono.fromCallable(() -> {
                    Student student = studentRepository.findById(studentId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "DB에 존재하지 않는 학생 ID입니다: " + studentId
                            ));

                    String latestFeedback = learningActivityRepository
                            .findFirstByStudentIdAndInstructorFeedbackIsNotNullOrderByStudyDateDesc(studentId)
                            .map(activity -> activity.getInstructorFeedback())
                            .orElse(null);

                    String weakConcept = weakSubject == null ? null
                            : wrongAnswerRepository.findTopWeaknessByStudentIdAndSubject(studentId, weakSubject).stream()
                                    .map(w -> w.getConceptTag())
                                    .filter(tag -> tag != null && !tag.isBlank())
                                    .findFirst()
                                    .orElse("");

                    return AiReportRequest.builder()
                            .studentId(studentId)
                            .currentKoreanGrade(student.getCurrentKoreanGrade() != null ? student.getCurrentKoreanGrade() : 0.0)
                            .studyTime(student.getStudyTime() != null ? student.getStudyTime() : 0.0)
                            .studentNote(student.getStudentNote() != null ? student.getStudentNote() : "")
                            .recommendContext(student.getRecommendContext() != null ? student.getRecommendContext() : "")
                            .instructorFeedback(latestFeedback != null ? latestFeedback : "")
                            .weakConcept(weakConcept)
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    // 🔄 공통 파이썬 통신 + 비동기 DB 저장 헬퍼 메서드
    private Mono<AiReportResponse> sendToPythonServer(String uri, AiReportRequest request, String reportTitle) {
        return sendToPythonServer(uriBuilder -> uriBuilder.path(uri).build(), uri, request, reportTitle);
    }

    private Mono<AiReportResponse> sendToPythonServer(Function<UriBuilder, URI> uriFunction, String uriForLog,
                                                      AiReportRequest request, String reportTitle) {
        // ⭕ 3분짜리 타임아웃 설정이 적용된 fastApiWebClient를 사용하도록 변경!
        return fastApiWebClient.post()
                .uri(uriFunction)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiReportResponse.class)
                .doOnSuccess(resource -> {
                    log.info("[AI Core 응답 완료] {} DB 저장을 시작합니다.", reportTitle);

                    Mono.fromRunnable(() -> {
                        AiRecommendation recommendation = AiRecommendation.builder()
                                .studentId(resource.getStudentId())
                                .title(reportTitle)
                                .careerAnalysis(resource.getCareerAnalysis())
                                .learningGuide(resource.getLearningGuide())
                                .createdAt(LocalDateTime.now())
                                .build();
                        aiRecommendationRepository.save(recommendation);
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                })
                .onErrorResume(error -> {
                    log.error("❌ [AI 서버 연결 실패] 주소: {}, 사유: {}", uriForLog, error.getMessage());
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "AI 분석 서버와 연결할 수 없습니다. 파이썬 백엔드 서버가 켜져 있는지 확인하세요."
                    ));
                });
    }

    public List<AiRecommendation> getReportsByStudentId(Long studentId) {
        log.info("[AI 진단 조회] 학생 ID {}의 과거 리포트 내역을 조회합니다.", studentId);
        return aiRecommendationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }
}