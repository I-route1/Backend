package com.i_route.backend.domain.ai.service;

import com.i_route.backend.domain.ai.dto.AiReportRequest;
import com.i_route.backend.domain.ai.dto.AiReportResponse;
import com.i_route.backend.domain.ai.entity.AiRecommendation;
import com.i_route.backend.domain.ai.entity.StudentInfo;
import com.i_route.backend.domain.ai.repository.AiRecommendationRepository;
import com.i_route.backend.domain.ai.repository.StudentInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
// ❌ @RequiredArgsConstructor 제거 (수동 생성자와의 충돌 방지)
public class AiCounselingService {

    private final WebClient fastApiWebClient; // 3분 타임아웃 설정을 담을 변수
    private final AiRecommendationRepository aiRecommendationRepository;
    private final StudentInfoRepository studentInfoRepository;

    // ⭕ 수동 생성자에서 @Qualifier로 3분짜리 빈을 정확히 매칭한 후, 위의 fastApiWebClient 필드에 대입합니다.
    public AiCounselingService(@Qualifier("fastApiWebClient") WebClient webClient,
                               AiRecommendationRepository aiRecommendationRepository,
                               StudentInfoRepository studentInfoRepository) {
        this.fastApiWebClient = webClient; // 👈 이름 매칭 수정!
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.studentInfoRepository = studentInfoRepository;
    }

    // 1️⃣ [수학 메타인지 모델 가동]
    public Mono<AiReportResponse> generateMathReport(String studentId) {
        log.info("📐 [수학 AI 가동] 학생 ID: {}의 진짜 데이터를 DB에서 조회합니다...", studentId);
        return fetchRealStudentData(studentId)
                .flatMap(realRequest -> sendToPythonServer("/api/ai/report/math", realRequest, "수학 메타인지 분석 리포트"));
    }

    // 2️⃣ [진로 탐색 및 작문 모델 가동]
    public Mono<AiReportResponse> generateWritingReport(String studentId) {
        log.info("✍️ [진로/작문 AI 가동] 학생 ID: {}의 진짜 데이터를 DB에서 조회합니다...", studentId);
        return fetchRealStudentData(studentId)
                .flatMap(realRequest -> sendToPythonServer("/api/ai/report/writing", realRequest, "인공지능 기반 진로 탐색 리포트"));
    }

    // 3️⃣ [프리미엄 통합 분석 리포트 가동]
    public Mono<AiReportResponse> generatePremiumReport(String studentId) {
        log.info("🚀 [통합 AI 가동] 학생 ID: {}의 진짜 데이터를 DB에서 조회합니다...", studentId);
        return fetchRealStudentData(studentId)
                .flatMap(realRequest -> sendToPythonServer("/api/ai/report/premium", realRequest, "i-Route 프리미엄 통합 리포트"));
    }

    // 🔍 [리액티브 특화 방어막] DB 블로킹 조회 격리
    private Mono<AiReportRequest> fetchRealStudentData(String studentId) {
        return Mono.fromCallable(() -> studentInfoRepository.findById(studentId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "DB에 존재하지 않는 학생 ID입니다: " + studentId
                        )))
                .subscribeOn(Schedulers.boundedElastic())
                .map(studentInfo -> AiReportRequest.builder()
                        .studentId(studentInfo.getStudentId())
                        .currentKoreanGrade(studentInfo.getCurrentKoreanGrade())
                        .studyTime(studentInfo.getStudyTime())
                        .studentNote(studentInfo.getStudentNote())
                        .recommendContext(studentInfo.getRecommendContext())
                        .build());
    }

    // 🔄 공통 파이썬 통신 + 비동기 DB 저장 헬퍼 메서드
    private Mono<AiReportResponse> sendToPythonServer(String uri, AiReportRequest request, String reportTitle) {
        // ⭕ 3분짜리 타임아웃 설정이 적용된 fastApiWebClient를 사용하도록 변경!
        return fastApiWebClient.post()
                .uri(uri)
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
                    log.error("❌ [AI 서버 연결 실패] 주소: {}, 사유: {}", uri, error.getMessage());
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "AI 분석 서버와 연결할 수 없습니다. 파이썬 백엔드 서버가 켜져 있는지 확인하세요."
                    ));
                });
    }

    public List<AiRecommendation> getReportsByStudentId(String studentId) {
        log.info("[AI 진단 조회] 학생 ID {}의 과거 리포트 내역을 조회합니다.", studentId);
        return aiRecommendationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }
}