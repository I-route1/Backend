package com.i_route.backend.ai.service;

import com.i_route.backend.ai.entity.ErrorType;
import com.i_route.backend.ai.entity.WrongAnswer;
import com.i_route.backend.ai.repository.WrongAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WrongAnswerService {

    private final WrongAnswerRepository wrongAnswerRepository;

    /**
     * ✍️ 1. 학생의 오답 이력을 실시간으로 기록 및 누적하는 로직
     */
    @Transactional
    public WrongAnswer recordWrongAnswer(String studentId, String subject, String questionId,
                                        String conceptTag, ErrorType errorType) {
        Optional<WrongAnswer> existingWrong = wrongAnswerRepository.findByStudentIdAndQuestionId(studentId, questionId);

        if (existingWrong.isPresent()) {
            WrongAnswer wrongAnswer = existingWrong.get();
            wrongAnswer.setFailCount(wrongAnswer.getFailCount() + 1);
            if (errorType != null) {
                wrongAnswer.setErrorType(errorType);
            }
            return wrongAnswerRepository.save(wrongAnswer);
        } else {
            WrongAnswer newWrong = WrongAnswer.builder()
                    .studentId(studentId)
                    .subject(subject)
                    .questionId(questionId)
                    .conceptTag(conceptTag)
                    .errorType(errorType)
                    .build();
            return wrongAnswerRepository.save(newWrong);
        }
    }

    /**
     * 📊 2. 특정 학생의 특정 과목 내 취약 개념 리스트를 조회 (AI RAG 연동용 기초 자원)
     */
    @Transactional(readOnly = true)
    public List<WrongAnswer> getAiTargetWeakness(String studentId, String subject) {
        return wrongAnswerRepository.findTopWeaknessByStudentIdAndSubject(studentId, subject);
    }
}