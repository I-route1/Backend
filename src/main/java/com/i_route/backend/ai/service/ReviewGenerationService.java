package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.ReviewPaperDto;
import com.i_route.backend.ai.entity.Question;
import com.i_route.backend.ai.entity.WrongAnswerEntity;
import com.i_route.backend.ai.repository.QuestionRepository;
import com.i_route.backend.ai.repository.WrongAnswerEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewGenerationService {

    private final WrongAnswerEntityRepository wrongAnswerRepository;
    private final QuestionRepository questionRepository;

    private static final AtomicLong paperIdCounter = new AtomicLong(1);

    public ReviewPaperDto generateCustomReviewPaper(Long studentId) {
        // 누적 오답 횟수 내림차순으로 상위 5개 취약 문제 추출
        List<WrongAnswerEntity> topWrongAnswers = wrongAnswerRepository
                .findByStudentIdOrderByFailCountDesc(studentId)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        if (topWrongAnswers.isEmpty()) {
            log.info("학생 {} 오답 데이터 없음. 빈 문제지 반환", studentId);
            return ReviewPaperDto.builder()
                    .paperId(paperIdCounter.getAndIncrement())
                    .questions(List.of())
                    .weakConcepts(List.of())
                    .build();
        }

        // 취약 문제의 questionId로 개념 태그 추출
        List<Long> questionIds = topWrongAnswers.stream()
                .map(WrongAnswerEntity::getQuestionId)
                .collect(Collectors.toList());

        List<Question> sourceQuestions = questionRepository.findAllById(questionIds);

        List<String> weakConcepts = sourceQuestions.stream()
                .map(Question::getConceptTag)
                .distinct()
                .collect(Collectors.toList());

        // 취약 개념 태그 기반 유사 문제(쌍둥이 문제) 추출
        List<String> reviewQuestions = weakConcepts.stream()
                .flatMap(tag -> questionRepository.findByConceptTag(tag).stream())
                .filter(q -> !questionIds.contains(q.getQuestionId()))  // 이미 틀린 문제는 제외
                .map(Question::getContent)
                .limit(10)
                .collect(Collectors.toList());

        log.info("학생 {} 맞춤 복습 문제지 생성 완료 - 취약 개념 {}개, 복습 문제 {}개",
                studentId, weakConcepts.size(), reviewQuestions.size());

        return ReviewPaperDto.builder()
                .paperId(paperIdCounter.getAndIncrement())
                .questions(reviewQuestions)
                .weakConcepts(weakConcepts)
                .build();
    }
}
