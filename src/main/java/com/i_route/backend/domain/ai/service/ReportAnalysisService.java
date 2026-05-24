package com.i_route.backend.domain.ai.service;

import com.i_route.backend.domain.ai.dto.AnalysisReportDto;
import com.i_route.backend.domain.ai.dto.MetaCognitionGapDto;
import com.i_route.backend.domain.ai.dto.StrengthAnalysisDto;
import com.i_route.backend.domain.ai.dto.StudyPatternDto;
import com.i_route.backend.domain.ai.entity.Grade;
import com.i_route.backend.domain.ai.entity.GradeEntity;
import com.i_route.backend.domain.ai.entity.LearningActivity;
import com.i_route.backend.domain.ai.entity.StudyLogEntity;
import com.i_route.backend.domain.ai.repository.GradeEntityRepository;
import com.i_route.backend.domain.ai.repository.GradeRepository;
import com.i_route.backend.domain.ai.repository.LearningActivityRepository;
import com.i_route.backend.domain.ai.repository.StudyLogRepository;
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
    private final LearningActivityRepository learningActivityRepository;

    // 기존 String 기반 API — 표준편차·전회 대비 변동 추가
    public AnalysisReportDto getAnalysisReport(String studentId, String subject) {
        List<Grade> grades = gradeRepository.findByStudentIdAndSubjectOrderByExamDateDesc(studentId, subject);

        if (grades.isEmpty()) {
            return null;
        }
        Grade latestGrade = grades.get(0);

        Double realAverageScore = gradeRepository.getAverageScoreBySubject(subject);
        double formattedAverage = (realAverageScore != null) ? Math.round(realAverageScore * 10) / 10.0 : 0.0;

        // 표준편차 계산
        List<Integer> scores = gradeRepository.findScoresByStudentAndSubject(studentId, subject);
        double stdDev = calculateStdDev(scores);

        // 전회 대비 점수 변동
        double scoreChange = 0.0;
        String trend = "유지";
        if (grades.size() >= 2) {
            scoreChange = grades.get(0).getScore() - grades.get(1).getScore();
            trend = scoreChange > 0 ? "상승" : (scoreChange < 0 ? "하락" : "유지");
        }

        return AnalysisReportDto.builder()
                .subjectName(latestGrade.getSubject())
                .myPercentile(latestGrade.getPercentile())
                .averageScore(formattedAverage)
                .standardDeviation(Math.round(stdDev * 10) / 10.0)
                .scoreChangeFromPrevious(scoreChange)
                .trendSummary(trend)
                .weakPointSummary(String.format("현재 %s 시험에서 '%s' 개념의 오답률이 높아 취약 단원으로 식별되었습니다.",
                        latestGrade.getExamType(), latestGrade.getWeakConceptTag()))
                .build();
    }

    private double calculateStdDev(List<Integer> scores) {
        if (scores.size() < 2) return 0.0;
        double mean = scores.stream().mapToInt(i -> i).average().orElse(0.0);
        double variance = scores.stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
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

    // 신규: 메타인지 괴리 분석 — 자기평가 vs 실제 성적 비교
    public MetaCognitionGapDto analyzeMetaCognitionGap(String studentId, String subject) {
        List<LearningActivity> activities = learningActivityRepository.findByStudentIdAndSubject(studentId, subject);
        List<Grade> grades = gradeRepository.findByStudentIdAndSubjectOrderByExamDateDesc(studentId, subject);

        if (activities.isEmpty() || grades.isEmpty()) {
            return MetaCognitionGapDto.builder()
                    .studentId(studentId)
                    .subject(subject)
                    .gapLevel("데이터 없음")
                    .gapSummary("학습 기록 또는 성적 데이터가 부족합니다.")
                    .advice("학습 기록을 꾸준히 입력해주세요.")
                    .build();
        }

        double avgUnderstanding = activities.stream()
                .mapToInt(LearningActivity::getUnderstandingScore)
                .average().orElse(0.0);

        double avgActual = grades.stream()
                .mapToInt(Grade::getScore)
                .average().orElse(0.0);

        double scaled = avgUnderstanding * 20.0; // 5점 → 100점 환산
        double gap = Math.abs(scaled - avgActual);

        String gapLevel;
        String summary;
        String advice;

        if (gap >= 20) {
            gapLevel = "HIGH";
            if (scaled > avgActual) {
                summary = String.format("자신의 이해도(%.0f점)를 과대평가하고 있습니다. 실제 성적(%.1f점)과 %.0f점 차이가 납니다.", scaled, avgActual, gap);
                advice = "자기 점검 문제풀이를 통해 실제 이해도를 재확인하세요. 개념 암기보다 응용 문제 풀이를 늘려보세요.";
            } else {
                summary = String.format("자신의 능력을 과소평가하고 있습니다. 실제 성적(%.1f점)이 자기평가(%.0f점)보다 %.0f점 높습니다.", avgActual, scaled, gap);
                advice = "자신감을 가지세요! 성취감을 느낄 수 있는 목표를 세우고 학습 효능감을 높여보세요.";
            }
        } else if (gap >= 10) {
            gapLevel = "MEDIUM";
            summary = String.format("자기평가(%.0f점)와 실제 성적(%.1f점) 간 %.0f점 차이가 있습니다.", scaled, avgActual, gap);
            advice = "오답 노트를 활용해 취약 개념을 구체적으로 파악하면 괴리를 줄일 수 있습니다.";
        } else {
            gapLevel = "LOW";
            summary = String.format("자기평가(%.0f점)와 실제 성적(%.1f점)이 거의 일치합니다. 메타인지가 우수합니다.", scaled, avgActual);
            advice = "현재의 자기 점검 습관을 유지하세요. 다른 과목에도 동일한 방식을 적용해 보세요.";
        }

        return MetaCognitionGapDto.builder()
                .studentId(studentId)
                .subject(subject)
                .avgUnderstandingScore(Math.round(avgUnderstanding * 10) / 10.0)
                .avgActualScore(Math.round(avgActual * 10) / 10.0)
                .scaledUnderstandingScore(Math.round(scaled * 10) / 10.0)
                .gapScore(Math.round(gap * 10) / 10.0)
                .gapLevel(gapLevel)
                .gapSummary(summary)
                .advice(advice)
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