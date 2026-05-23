package com.i_route.backend.service;

import com.i_route.backend.dto.AnalysisReportDto;
import com.i_route.backend.dto.StrengthAnalysisDto;
import com.i_route.backend.dto.StudyPatternDto;
import com.i_route.backend.entity.Grade;
import com.i_route.backend.entity.GradeEntity;
import com.i_route.backend.entity.StudyLogEntity;
import com.i_route.backend.repository.GradeEntityRepository;
import com.i_route.backend.repository.GradeRepository;
import com.i_route.backend.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAnalysisService {

    private final GradeRepository gradeRepository;
    private final GradeEntityRepository gradeEntityRepository;
    private final StudyLogRepository studyLogRepository;

    // 기존 String 기반 API 유지
    public AnalysisReportDto getAnalysisReport(String studentId, String subject) {
        List<Grade> grades = gradeRepository.findByStudentIdAndSubjectOrderByExamDateDesc(studentId, subject);

        if (grades.isEmpty()) {
            return null;
        }
        Grade latestGrade = grades.get(0);

        Double realAverageScore = gradeRepository.getAverageScoreBySubject(subject);
        double formattedAverage = (realAverageScore != null) ? Math.round(realAverageScore * 10) / 10.0 : 0.0;

        return AnalysisReportDto.builder()
                .subjectName(latestGrade.getSubject())
                .myPercentile(latestGrade.getPercentile())
                .averageScore(formattedAverage)
                .weakPointSummary(String.format("현재 %s 시험에서 '%s' 개념의 오답률이 높아 취약 단원으로 식별되었습니다.",
                        latestGrade.getExamType(), latestGrade.getWeakConceptTag()))
                .build();
    }

    // 신규: Long 기반 특정 시험 종합 리포트
    public AnalysisReportDto generateScoreReport(Long studentId, Long testId) {
        GradeEntity grade = gradeEntityRepository.findById(testId)
                .filter(g -> g.getStudentId().equals(studentId))
                .orElseThrow(() -> new IllegalArgumentException("해당 시험 데이터를 찾을 수 없습니다."));

        List<GradeEntity> allGrades = gradeEntityRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(
                studentId, grade.getSubjectId());

        double avg = allGrades.stream()
                .mapToInt(GradeEntity::getScore)
                .average()
                .orElse(0.0);

        return AnalysisReportDto.builder()
                .subjectName("과목 ID: " + grade.getSubjectId())
                .myPercentile(grade.getPercentile())
                .averageScore(Math.round(avg * 10) / 10.0)
                .weakPointSummary(String.format("해당 시험 점수: %d점, 과목 평균: %.1f점", grade.getScore(), avg))
                .build();
    }

    // 신규: 학습 패턴 분석 (골든타임 + 과목별 학습량)
    public StudyPatternDto analyzeStudyPattern(Long studentId) {
        List<StudyLogEntity> logs = studyLogRepository.findByStudentId(studentId);

        if (logs.isEmpty()) {
            return StudyPatternDto.builder()
                    .studentId(studentId)
                    .goldenTime("데이터 없음")
                    .subjectStudyMinutes(Map.of())
                    .studyBalanceSummary("아직 학습 기록이 없습니다.")
                    .build();
        }

        // 시간대별 학습 분 집계 → 가장 많은 시간대 = 골든타임
        Map<Integer, Integer> hourlyMinutes = logs.stream()
                .filter(l -> l.getStartTime() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getStartTime().getHour(),
                        Collectors.summingInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0)
                ));

        String goldenTime = hourlyMinutes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + "시~" + (e.getKey() + 1) + "시")
                .orElse("분석 불가");

        // 과목별 총 학습 시간
        Map<Long, Integer> subjectMinutes = logs.stream()
                .collect(Collectors.groupingBy(
                        StudyLogEntity::getSubjectId,
                        Collectors.summingInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0)
                ));

        int totalMinutes = subjectMinutes.values().stream().mapToInt(Integer::intValue).sum();
        String summary = String.format("총 학습 시간 %d분 | 집중 골든타임: %s | 과목 수: %d개",
                totalMinutes, goldenTime, subjectMinutes.size());

        log.info("학생 {} 학습 패턴 분석 완료 - 골든타임: {}", studentId, goldenTime);

        return StudyPatternDto.builder()
                .studentId(studentId)
                .goldenTime(goldenTime)
                .subjectStudyMinutes(subjectMinutes)
                .studyBalanceSummary(summary)
                .build();
    }

    // 신규: 강점 영역 분석 (백분위 70 이상 과목)
    public StrengthAnalysisDto analyzeStrengths(Long studentId) {
        List<GradeEntity> grades = gradeEntityRepository.findByStudentIdOrderByTestDateAsc(studentId);

        Map<Long, Double> avgPercentileBySubject = grades.stream()
                .filter(g -> g.getPercentile() != null)
                .collect(Collectors.groupingBy(
                        GradeEntity::getSubjectId,
                        Collectors.averagingDouble(GradeEntity::getPercentile)
                ));

        List<Long> strongSubjects = avgPercentileBySubject.entrySet().stream()
                .filter(e -> e.getValue() >= 70.0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        String summary = strongSubjects.isEmpty()
                ? "아직 두드러진 강점 과목이 없습니다. 꾸준한 학습이 필요합니다."
                : "과목 ID " + strongSubjects + "에서 백분위 70 이상의 안정적인 성과를 보이고 있습니다!";

        log.info("학생 {} 강점 과목 {}개 식별 완료", studentId, strongSubjects.size());

        return StrengthAnalysisDto.builder()
                .studentId(studentId)
                .strongSubjectIds(strongSubjects)
                .strengthSummary(summary)
                .build();
    }
}