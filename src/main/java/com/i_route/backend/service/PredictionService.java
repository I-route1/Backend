package com.i_route.backend.service;

import com.i_route.backend.dto.PredictedScoreDto;
import com.i_route.backend.entity.GradeEntity;
import com.i_route.backend.entity.StudyLogEntity;
import com.i_route.backend.repository.GradeEntityRepository;
import com.i_route.backend.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final GradeEntityRepository gradeRepository;
    private final StudyLogRepository studyLogRepository;

    public PredictedScoreDto predictNextScore(Long studentId, Long subjectId) {
        List<GradeEntity> grades = gradeRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(studentId, subjectId);

        if (grades.size() < 2) {
            log.info("학생 {} 과목 {} 성적 데이터 부족 ({}건). 기본 예측값 반환", studentId, subjectId, grades.size());
            int baseScore = grades.isEmpty() ? 50 : grades.get(0).getScore();
            return PredictedScoreDto.builder()
                    .expectedScoreMin(Math.max(0, baseScore - 10))
                    .expectedScoreMax(Math.min(100, baseScore + 10))
                    .achievementProbability(50.0)
                    .build();
        }

        // 선형 회귀: 날짜 기준 점수 추세 계산
        LocalDate baseDate = grades.get(0).getTestDate();
        int n = grades.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (GradeEntity g : grades) {
            double x = ChronoUnit.DAYS.between(baseDate, g.getTestDate());
            double y = g.getScore();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // 다음 시험까지 30일 후로 가정
        double nextX = ChronoUnit.DAYS.between(baseDate, LocalDate.now().plusDays(30));
        double predicted = slope * nextX + intercept;
        predicted = Math.max(0, Math.min(100, predicted));

        // 최근 2주 학습량 보너스: 평균 이상이면 확률 상승
        List<StudyLogEntity> recentLogs = studyLogRepository.findByStudentId(studentId);
        double avgMinutes = recentLogs.stream()
                .mapToInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0)
                .average()
                .orElse(0);

        double studyBonus = avgMinutes >= 60 ? 10.0 : avgMinutes >= 30 ? 5.0 : 0.0;
        double probability = Math.min(95.0, 50.0 + slope * 5 + studyBonus);

        int scoreMin = (int) Math.max(0, predicted - 7);
        int scoreMax = (int) Math.min(100, predicted + 7);

        log.info("학생 {} 과목 {} 예상 점수: {}~{}, 달성 확률: {}%",
                studentId, subjectId, scoreMin, scoreMax, String.format("%.1f", probability));

        return PredictedScoreDto.builder()
                .expectedScoreMin(scoreMin)
                .expectedScoreMax(scoreMax)
                .achievementProbability(Math.round(probability * 10) / 10.0)
                .build();
    }
}
