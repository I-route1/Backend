package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.GradeAnalysisResponse;
import com.i_route.backend.ai.dto.GradeRequest;
import com.i_route.backend.ai.dto.GradeResponse;
import com.i_route.backend.ai.entity.Grade;
import com.i_route.backend.ai.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    // 1. 성적 저장 기능
    @Transactional
    public GradeResponse saveGrade(GradeRequest request) {
        Grade grade = Grade.builder()
                .studentId(request.getStudentId())
                .subject(request.getSubject())
                .score(request.getScore())
                .gradeLevel(request.getGradeLevel())
                .examType(request.getExamType())
                .examDate(request.getExamDate())
                .build();

        Grade savedGrade = gradeRepository.save(grade);
        return GradeResponse.from(savedGrade);
    }

    // 2. 성적 기본 조회 기능 (최신순)
    @Transactional(readOnly = true)
    public List<GradeResponse> getGradesByStudent(String studentId) {
        return gradeRepository.findByStudentIdOrderByExamDateDesc(studentId)
                .stream()
                .map(GradeResponse::from)
                .collect(Collectors.toList());
    }

    // 🌟 [NEW] 3. 시계열 및 전월 대비 성적 변동폭 분석 로직
    @Transactional(readOnly = true)
    public GradeAnalysisResponse analyzeStudentGrades(String studentId) {
        // 해당 학생의 모든 성적 가져오기
        List<Grade> allGrades = gradeRepository.findByStudentIdOrderByExamDateDesc(studentId);

        if (allGrades.isEmpty()) {
            return GradeAnalysisResponse.builder()
                    .studentId(studentId)
                    .gradeHistory(Collections.emptyList())
                    .subjectScoreChanges(Collections.emptyMap())
                    .summaryMessage("아직 등록된 성적 데이터가 없습니다.")
                    .build();
        }

        // 그래프용 데이터: 날짜 오래된 순(오름차순)으로 반환하기 위해 역정렬
        List<GradeResponse> history = allGrades.stream()
                .map(GradeResponse::from)
                .sorted(Comparator.comparing(GradeResponse::getExamDate))
                .collect(Collectors.toList());

        // 과목별로 시험들을 묶기 (과목별 날짜 내림차순 정렬 상태가 됨)
        Map<String, List<Grade>> gradesBySubject = allGrades.stream()
                .collect(Collectors.groupingBy(Grade::getSubject));

        Map<String, Integer> scoreChanges = new HashMap<>();
        StringBuilder summaryBuilder = new StringBuilder();

        // 과목별 점수 변동 계산 (최근 시험 점수 - 직전 시험 점수)
        for (Map.Entry<String, List<Grade>> entry : gradesBySubject.entrySet()) {
            String subject = entry.getKey();
            List<Grade> subjectGrades = entry.getValue();

            if (subjectGrades.size() >= 2) {
                // 내림차순이므로 0번이 가장 최근 시험, 1번이 직전 시험
                int latestScore = subjectGrades.get(0).getScore();
                int previousScore = subjectGrades.get(1).getScore();
                int change = latestScore - previousScore;

                scoreChanges.put(subject, change);

                if (change > 0) {
                    summaryBuilder.append(String.format("지난 시험 대비 [%s] 성적이 %d점 상승했습니다! 🎉 ", subject, change));
                } else if (change < 0) {
                    summaryBuilder.append(String.format("지난 시험 대비 [%s] 성적이 %d점 하락했습니다. 취약 단원 보완이 필요합니다. 📉 ", subject, Math.abs(change)));
                } else {
                    summaryBuilder.append(String.format("[%s] 성적이 지난 시험과 동일하게 유지되었습니다. ➖ ", subject));
                }
            } else {
                // 시험이 하나밖에 없는 과목
                scoreChanges.put(subject, 0);
            }
        }

        if (summaryBuilder.length() == 0) {
            summaryBuilder.append("첫 시험 성적이 등록되었습니다. 다음 시험부터 추이 분석이 제공됩니다!");
        }

        return GradeAnalysisResponse.builder()
                .studentId(studentId)
                .gradeHistory(history)
                .subjectScoreChanges(scoreChanges)
                .summaryMessage(summaryBuilder.toString().trim())
                .build();
    }
}