package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.ReviewPaperDto;
import com.i_route.backend.ai.entity.GradeEntity;
import com.i_route.backend.ai.entity.Question;
import com.i_route.backend.ai.entity.WrongAnswerEntity;
import com.i_route.backend.ai.repository.GradeEntityRepository;
import com.i_route.backend.ai.repository.QuestionRepository;
import com.i_route.backend.ai.repository.WrongAnswerEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewGenerationService {

    private final WrongAnswerEntityRepository wrongAnswerRepository;
    private final QuestionRepository questionRepository;
    private final GradeEntityRepository gradeEntityRepository;

    private static final AtomicLong paperIdCounter = new AtomicLong(1);

    public ReviewPaperDto generateCustomReviewPaper(Long studentId) {
        // 누적 오답 횟수 내림차순으로 상위 5개 취약 문제 추출
        List<WrongAnswerEntity> topWrongAnswers = wrongAnswerRepository
                .findByStudentIdOrderByFailCountDesc(studentId)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        if (topWrongAnswers.isEmpty()) {
            return generateFallbackPaper(studentId);
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
                .filter(q -> !questionIds.contains(q.getQuestionId()))
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

    // 오답 데이터 없을 때 GradeEntity 기반 취약 과목 문제로 대체
    private ReviewPaperDto generateFallbackPaper(Long studentId) {
        // 가장 낮은 점수의 과목 ID 찾기
        List<GradeEntity> grades = gradeEntityRepository.findByStudentIdOrderByTestDateAsc(studentId);

        Long weakestSubjectId = 2L; // 기본값: 수학
        if (!grades.isEmpty()) {
            weakestSubjectId = grades.stream()
                    .filter(g -> g.getScore() != null)
                    .min(Comparator.comparingInt(GradeEntity::getScore))
                    .map(GradeEntity::getSubjectId)
                    .orElse(2L);
        }

        String subjectName = switch (weakestSubjectId.intValue()) {
            case 1 -> "국어"; case 2 -> "수학"; case 3 -> "영어";
            case 4 -> "과학"; case 5 -> "사회"; default -> "수학";
        };

        final Long subjectId = weakestSubjectId;
        List<Question> fallbackQuestions = questionRepository.findAll().stream()
                .filter(q -> subjectId.equals(q.getSubjectId()))
                .limit(10)
                .collect(Collectors.toList());

        if (fallbackQuestions.isEmpty()) {
            // 과목 무관 전체 문제 사용
            fallbackQuestions = questionRepository.findAll().stream().limit(10).collect(Collectors.toList());
        }

        List<String> weakConcepts = fallbackQuestions.stream()
                .map(Question::getConceptTag)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .limit(3)
                .collect(Collectors.toList());

        List<String> reviewQuestions = fallbackQuestions.stream()
                .map(Question::getContent)
                .collect(Collectors.toList());

        log.info("학생 {} 오답 데이터 없음 → {}(과목ID:{}) 기반 {} 문제 생성",
                studentId, subjectName, weakestSubjectId, reviewQuestions.size());

        return ReviewPaperDto.builder()
                .paperId(paperIdCounter.getAndIncrement())
                .questions(reviewQuestions)
                .weakConcepts(weakConcepts.isEmpty()
                        ? List.of(subjectName + " 기초 개념") : weakConcepts)
                .build();
    }
}
