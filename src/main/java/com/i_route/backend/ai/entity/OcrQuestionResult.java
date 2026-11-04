package com.i_route.backend.ai.entity;

import com.i_route.backend.ai.dto.OcrResultRequest;
import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OcrQuestionResult {
    @Column(nullable = false)
    private String questionId;
    @Lob @Column(nullable = false)
    private String expectedAnswer;
    @Lob @Column(nullable = false)
    private String recognizedText;
    @Column(nullable = false)
    private double similarity;
    @Column(nullable = false)
    private boolean correct;
    @Column(nullable = false)
    private String conceptTag;
    @Enumerated(EnumType.STRING)
    private ErrorType errorType;

    public OcrQuestionResult(OcrResultRequest.Question question) {
        questionId = question.questionId();
        expectedAnswer = question.expectedAnswer();
        recognizedText = question.recognizedText();
        similarity = question.similarity();
        correct = question.correct();
        conceptTag = question.conceptTag();
        errorType = question.errorType();
    }

    public OcrResultRequest.Question toDto() {
        return new OcrResultRequest.Question(questionId, expectedAnswer, recognizedText,
                similarity, correct, conceptTag, errorType);
    }
}
