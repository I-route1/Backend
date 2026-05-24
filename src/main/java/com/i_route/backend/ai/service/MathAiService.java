package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.AiDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MathAiService {

    private final WebClient fastApiWebClient;
    private final WebClient ollamaWebClient;

    public Mono<String> getMathExplanation(String userQuestion) {

        // 1단계: 파이썬 FastAPI로 out_db(FAISS) 지식 족보 검색
        return fastApiWebClient.post()
                .uri("/api/rag/search")
                .bodyValue(new AiDto.Request(userQuestion))
                .retrieve()
                .bodyToMono(AiDto.RagResponse.class)
                .flatMap(rag -> {

                    // 2단계: 가져온 족보(Context)와 질문을 섞어 프롬프트 완성
                    String finalPrompt = String.format("""
                            [참고 수학 족보 데이터]
                            %s
                            
                            [학생의 질문]
                            %s
                            
                            위 참고 족보의 풀이 스타일과 LaTeX 수식 형식을 엄격히 준수하여 학생의 질문에 친절하게 답변해줘.
                            """, rag.getContext(), userQuestion);

                    // Ollama에 보낼 포맷 세팅 (stream은 단답형으로 우선 false)
                    AiDto.OllamaRequest ollamaRequest = new AiDto.OllamaRequest(
                            "iroute-math-model", // ✏️ 허깅페이스에 올린 수학 모델을 로컬 Ollama에 등록한 이름
                            finalPrompt,
                            false
                    );

                    // 3단계: Ollama 뇌세포로 최종 추론 때리기
                    return ollamaWebClient.post()
                            .uri("/api/generate")
                            .bodyValue(ollamaRequest)
                            .retrieve()
                            .bodyToMono(AiDto.OllamaResponse.class)
                            .map(AiDto.OllamaResponse::getResponse);
                });
    }
}