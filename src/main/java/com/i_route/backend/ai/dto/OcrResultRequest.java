package com.i_route.backend.ai.dto;

import com.i_route.backend.ai.entity.ErrorType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OcrResultRequest(
        @NotNull @Positive Long studentId,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 64) String submissionId,
        @NotNull @Positive Integer totalQuestions,
        @NotNull @Min(0) Integer correctCount,
        @NotEmpty @Size(max = 1000) List<@NotNull @Valid Question> questions) {

    public record Question(
            @NotBlank @Size(max = 255) String questionId,
            @NotNull @Size(max = 10000) String expectedAnswer,
            @NotNull @Size(max = 10000) String recognizedText,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double similarity,
            @NotNull Boolean correct,
            @NotBlank @Size(max = 255) String conceptTag,
            ErrorType errorType) {}

    @AssertTrue(message = "총 문항 수·정답 수는 문항별 결과와 일치하고 문항 ID는 중복되지 않아야 합니다.")
    @JsonIgnore
    public boolean isSummaryValid() {
        if (questions == null || totalQuestions == null || correctCount == null
                || questions.stream().anyMatch(q -> q == null || q.similarity() == null || !Double.isFinite(q.similarity()))) {
            return false;
        }
        return totalQuestions == questions.size()
                && correctCount == questions.stream().filter(q -> Boolean.TRUE.equals(q.correct())).count()
                && questions.stream().map(Question::questionId).distinct().count() == questions.size();
    }
}
