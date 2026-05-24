package com.i_route.backend.domain.ai.service;

import com.i_route.backend.domain.ai.entity.WrongAnswer;
import com.i_route.backend.domain.ai.repository.WrongAnswerRepository;
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
    public WrongAnswer recordWrongAnswer(String studentId, String subject, String questionId, String conceptTag) {
        // 이미 해당 학생이 과거에 동일한 문제를 틀린 적이 있는지 DB에서 조회
        Optional<WrongAnswer> existingWrong = wrongAnswerRepository.findByStudentIdAndQuestionId(studentId, questionId);

        if (existingWrong.isPresent()) {
            // [CASE A] 이미 틀렸던 문제라면 -> 오답 횟수(failCount)를 +1 가중치 부여
            WrongAnswer wrongAnswer = existingWrong.get();
            wrongAnswer.setFailCount(wrongAnswer.getFailCount() + 1);
            return wrongAnswerRepository.save(wrongAnswer);
        } else {
            // [CASE B] 처음 틀린 새로운 변형 문제라면 -> 새 오답 로그 생성
            WrongAnswer newWrong = WrongAnswer.builder()
                    .studentId(studentId)
                    .subject(subject)
                    .questionId(questionId)
                    .conceptTag(conceptTag)
                    .build(); // prePersist에 의해 failCount는 자동으로 1이 세팅됩니다.
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