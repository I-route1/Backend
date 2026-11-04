package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.entity.ErrorType;
import com.i_route.backend.ai.entity.WrongAnswer;
import com.i_route.backend.ai.service.WrongAnswerService;
import com.i_route.backend.ai.service.StudentRecordAccess;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wrong-answer")
@RequiredArgsConstructor
@Validated
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;
    private final StudentRecordAccess access;

    /** 학생의 전체 오답. subject를 생략하면 모든 과목을 최신 등록순으로 반환한다. */
    @GetMapping("/{studentId}")
    public ResponseEntity<List<WrongAnswer>> getWrongAnswers(
            @PathVariable Long studentId,
            @RequestParam(required = false) String subject) {
        access.requireStudent(studentId);
        return ResponseEntity.ok(wrongAnswerService.getWrongAnswers(studentId, subject));
    }

    /**
     * ✍️ 프론트 없이 HTTP 파일로 오답을 직접 DB에 쏴보는 테스트용 API
     */
    @PostMapping("/record")
    public ResponseEntity<WrongAnswer> recordWrong(
            @RequestParam Long studentId,
            @RequestParam @NotBlank @Size(max = 255) String subject,
            @RequestParam @NotBlank @Size(max = 255) String questionId,
            @RequestParam @NotBlank @Size(max = 255) String conceptTag,
            @RequestParam(required = false) ErrorType errorType) {

        access.requireStudent(studentId);
        WrongAnswer recorded = wrongAnswerService.recordWrongAnswer(studentId, subject, questionId, conceptTag, errorType);
        return ResponseEntity.ok(recorded);
    }

    /**
     * 🤖 [안전하게 내부로 진입] 파이썬 AI 서버가 특정 학생의 오답 데이터를 요청하는 파이프라인 API
     */
    @GetMapping("/ai-pipeline")
    public ResponseEntity<List<WrongAnswer>> getAiPipelineData(
            @RequestParam Long studentId,
            @RequestParam String subject) {

        List<WrongAnswer> weaknessList = wrongAnswerService.getAiTargetWeakness(studentId, subject);
        return ResponseEntity.ok(weaknessList);
    }
}
