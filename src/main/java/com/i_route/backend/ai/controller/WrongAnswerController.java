package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.entity.WrongAnswer;
import com.i_route.backend.ai.service.WrongAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List; // 🔥 List 사용을 위해 추가된 임포트

@RestController
@RequestMapping("/api/wrong-answer")
@RequiredArgsConstructor
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;

    /**
     * ✍️ 프론트 없이 HTTP 파일로 오답을 직접 DB에 쏴보는 테스트용 API
     */
    @PostMapping("/record")
    public Mono<ResponseEntity<WrongAnswer>> recordWrong(
            @RequestParam String studentId,
            @RequestParam String subject,
            @RequestParam String questionId,
            @RequestParam String conceptTag) {

        WrongAnswer recorded = wrongAnswerService.recordWrongAnswer(studentId, subject, questionId, conceptTag);
        return Mono.just(ResponseEntity.ok(recorded));
    }

    /**
     * 🤖 [안전하게 내부로 진입] 파이썬 AI 서버가 특정 학생의 오답 데이터를 요청하는 파이프라인 API
     */
    @GetMapping("/ai-pipeline")
    public Mono<ResponseEntity<List<WrongAnswer>>> getAiPipelineData(
            @RequestParam String studentId,
            @RequestParam String subject) {

        List<WrongAnswer> weaknessList = wrongAnswerService.getAiTargetWeakness(studentId, subject);
        return Mono.just(ResponseEntity.ok(weaknessList));
    }
}