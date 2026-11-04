package com.i_route.backend.ai.dto;

import com.i_route.backend.ai.entity.OcrGradingResult;
import com.i_route.backend.ai.entity.OcrQuestionResult;
import java.time.Instant;
import java.util.List;

public record OcrResultResponse(Long id, Long studentId, String subject, String submissionId,
                                int totalQuestions, int correctCount, Instant gradedAt,
                                List<OcrResultRequest.Question> questions) {
    public static OcrResultResponse from(OcrGradingResult result) {
        return new OcrResultResponse(result.getId(), result.getStudentId(), result.getSubject(),
                result.getSubmissionId(), result.getTotalQuestions(), result.getCorrectCount(), result.getGradedAt(),
                result.getQuestions().stream().map(OcrQuestionResult::toDto).toList());
    }
}
