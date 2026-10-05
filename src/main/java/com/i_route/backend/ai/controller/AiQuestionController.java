package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.AiQuestionDto;
import com.i_route.backend.ai.service.AiQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiQuestionService aiQuestionService;

    /**
     * POST /api/ai/ask (로그인 필요)
     * Body: { question, subject? }  — subject: 수학·영어·국어·과학·사회, 비우면 자동 감지
     * Response: { subject, question, answer, grounded, needSubject }
     *   needSubject=true: 과목을 알 수 없어 답하지 않음 → 과목을 골라 다시 요청
     * 400: 빈 질문·300자 초과·미지원 과목, 503: AI 서버가 답하지 못함(꺼짐·50초 초과)
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody AiQuestionDto.Request request) {
        return ResponseEntity.ok(aiQuestionService.ask(request.getQuestion(), request.getSubject()));
    }
}
