package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.entity.ErrorType;
import com.i_route.backend.ai.entity.WrongAnswer;
import com.i_route.backend.ai.service.WrongAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wrong-answer")
@RequiredArgsConstructor
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;

    /**
     * ✍️ 프론트 없이 HTTP 파일로 오답을 직접 DB에 쏴보는 테스트용 API
     */
    @PostMapping("/record")
    public ResponseEntity<WrongAnswer> recordWrong(
            @RequestParam String studentId,
            @RequestParam String subject,
            @RequestParam String questionId,
            @RequestParam String conceptTag,
            @RequestParam(required = false) ErrorType errorType) {

        WrongAnswer recorded = wrongAnswerService.recordWrongAnswer(studentId, subject, questionId, conceptTag, errorType);
        return ResponseEntity.ok(recorded);
    }

    /**
     * 🤖 [안전하게 내부로 진입] 파이썬 AI 서버가 특정 학생의 오답 데이터를 요청하는 파이프라인 API
     */
    @GetMapping("/ai-pipeline")
    public ResponseEntity<List<WrongAnswer>> getAiPipelineData(
            @RequestParam String studentId,
            @RequestParam String subject) {

        List<WrongAnswer> weaknessList = wrongAnswerService.getAiTargetWeakness(studentId, subject);
        return ResponseEntity.ok(weaknessList);
    }
}