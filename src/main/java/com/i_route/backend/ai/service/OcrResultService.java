package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.OcrResultRequest;
import com.i_route.backend.ai.dto.OcrResultResponse;
import com.i_route.backend.ai.entity.OcrGradingResult;
import com.i_route.backend.ai.repository.OcrGradingResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OcrResultService {
    private final OcrGradingResultRepository repository;
    private final WrongAnswerService wrongAnswerService;

    /** 결과와 오답을 같은 트랜잭션에 저장. 네트워크 재전송은 오답 횟수를 늘리지 않는다. */
    @Transactional
    public OcrResultResponse save(OcrResultRequest request) {
        var existing = repository.findByStudentIdAndSubmissionId(request.studentId(), request.submissionId());
        if (existing.isPresent()) {
            var result = OcrResultResponse.from(existing.get());
            if (!result.subject().equals(request.subject()) || result.totalQuestions() != request.totalQuestions()
                    || result.correctCount() != request.correctCount() || !result.questions().equals(request.questions())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "submissionId가 다른 채점 결과에 이미 사용되었습니다.");
            }
            return result;
        }
        var saved = repository.save(new OcrGradingResult(request));
        for (var question : request.questions()) {
            if (!question.correct()) {
                wrongAnswerService.recordWrongAnswer(request.studentId(), request.subject(), question.questionId(),
                        question.conceptTag(), question.errorType());
            }
        }
        return OcrResultResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OcrResultResponse> getByStudent(Long studentId, String subject) {
        var results = subject == null || subject.isBlank()
                ? repository.findByStudentIdOrderByGradedAtDescIdDesc(studentId)
                : repository.findByStudentIdAndSubjectOrderByGradedAtDescIdDesc(studentId, subject);
        return results.stream().map(OcrResultResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OcrResultResponse get(Long id, Long studentId) {
        return OcrResultResponse.from(repository.findByIdAndStudentId(id, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OCR 채점 결과를 찾을 수 없습니다.")));
    }
}
