package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.MaterialRecommendationDto;
import com.i_route.backend.ai.dto.PeerSuccessPathDto;
import com.i_route.backend.ai.dto.StudyMethodDto;
import com.i_route.backend.ai.dto.StudyRoadmapDto;
import com.i_route.backend.ai.entity.Grade;
import com.i_route.backend.ai.entity.StudentEntity;
import com.i_route.backend.ai.entity.StudyTendency;
import com.i_route.backend.ai.entity.WrongAnswerEntity;
import com.i_route.backend.ai.repository.GradeRepository;
import com.i_route.backend.ai.repository.StudentEntityRepository;
import com.i_route.backend.ai.repository.StudyMaterialRepository;
import com.i_route.backend.ai.repository.TargetGoalRepository;
import com.i_route.backend.ai.repository.WrongAnswerEntityRepository;
import com.i_route.backend.ai.entity.TargetGoalEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final StudentEntityRepository studentRepository;
    private final StudyMaterialRepository materialRepository;
    private final WrongAnswerEntityRepository wrongAnswerRepository;
    private final TargetGoalRepository targetGoalRepository;
    private final GradeRepository gradeRepository;

    public List<MaterialRecommendationDto> recommendMaterials(Long studentId) {
        StudentEntity student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            log.warn("학생을 찾을 수 없어 빈 자료 목록을 반환합니다: {}", studentId);
            return List.of();
        }

        StudyTendency tendency = student.getLearningTendency();
        String tendencyDesc = switch (tendency != null ? tendency : StudyTendency.VISUAL) {
            case VISUAL      -> "시각형 학습자에게 적합한";
            case AUDITORY    -> "청각형 학습자를 위한";
            case KINESTHETIC -> "행동형 학습자에게 효과적인";
        };

        List<MaterialRecommendationDto> result = materialRepository
                .findByLevelLessThanEqualOrderByLevelDesc(student.getCurrentLevel())
                .stream()
                .map(m -> {
                    String typeDesc = "교재".equals(m.getMaterialType())
                            ? "개념 정리와 문제풀이를 직접 해볼 수 있는"
                            : "강의로 빠르게 개념을 습득할 수 있는";
                    String levelHint = m.getLevel() == student.getCurrentLevel()
                            ? " (현재 레벨 딱 맞춤)"
                            : " (레벨 " + m.getLevel() + " → 현재 " + student.getCurrentLevel() + " 준비)";
                    return MaterialRecommendationDto.builder()
                            .materialId(m.getMaterialId())
                            .title(m.getTitle())
                            .materialType(m.getMaterialType())
                            .matchReason(tendencyDesc + " " + typeDesc + " 자료" + levelHint)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("학생 {} 맞춤 자료 {}건 추천 완료", studentId, result.size());
        return result;
    }

    public StudyMethodDto suggestStudyMethod(Long studentId, Long subjectId) {
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다: " + studentId));

        StudyTendency tendency = student.getLearningTendency();
        String guide;
        String approach;

        if (tendency == StudyTendency.VISUAL) {
            guide = "개념 마인드맵, 색깔 분류, 도표 정리를 활용하세요.";
            approach = "유튜브 강의 + 개념 노트 시각화";
        } else if (tendency == StudyTendency.AUDITORY) {
            guide = "강의 듣기 중심으로, 소리 내어 개념을 복창하고 그룹 스터디를 활용하세요.";
            approach = "음성 강의 반복 청취 + 설명 연습";
        } else {
            guide = "직접 문제 풀이 중심으로, 실습과 반복 훈련을 통해 체득하세요.";
            approach = "문제집 반복 풀이 + 오답 즉시 복습";
        }

        return StudyMethodDto.builder()
                .studentId(studentId)
                .subjectId(subjectId)
                .tendency(tendency != null ? tendency.name() : "미설정")
                .studyGuide(guide)
                .recommendedApproach(approach)
                .build();
    }

    public StudyRoadmapDto recommendGoalRoadmap(Long studentId) {
        TargetGoalEntity goal = targetGoalRepository.findByStudentId(studentId)
                .orElse(null);

        if (goal == null) {
            return StudyRoadmapDto.builder()
                    .studentId(studentId)
                    .targetKeyword("미설정")
                    .weeklyMilestones(List.of())
                    .overallStrategy("아직 학습 목표가 등록되지 않았습니다. 목표를 먼저 설정해주세요.")
                    .build();
        }

        long remainingDays = LocalDate.now().until(goal.getTargetDate()).getDays();
        long remainingWeeks = Math.max(remainingDays / 7, 1);

        List<String> milestones = new ArrayList<>();
        long phases = Math.min(remainingWeeks, 8);
        for (int i = 1; i <= phases; i++) {
            if (i <= phases / 3) {
                milestones.add(i + "주차: 기초 개념 정리 및 취약 단원 집중 보완");
            } else if (i <= phases * 2 / 3) {
                milestones.add(i + "주차: 유형별 문제 풀이 및 오답 노트 정리");
            } else {
                milestones.add(i + "주차: 실전 모의고사 및 최종 마무리 점검");
            }
        }

        return StudyRoadmapDto.builder()
                .studentId(studentId)
                .targetKeyword(goal.getTargetKeyword())
                .targetDate(goal.getTargetDate())
                .weeklyMilestones(milestones)
                .overallStrategy("목표 [" + goal.getTargetKeyword() + "] 달성을 위해 하루 " +
                        goal.getDailyStudyHours() + "시간 기준 " + remainingWeeks + "주 로드맵")
                .build();
    }

    // 유사 성적대 사용자들이 선호하는 콘텐츠 매칭
    public List<MaterialRecommendationDto> recommendByPeerContent(String studentId) {
        Double myAvg = gradeRepository.getAverageScoreByStudent(studentId);
        if (myAvg == null) {
            return List.of();
        }

        List<Object[]> allAvgs = gradeRepository.findAllStudentAverageScores();
        long peerCount = allAvgs.stream()
                .filter(row -> {
                    String sid = (String) row[0];
                    double avg = ((Number) row[1]).doubleValue();
                    return !sid.equals(studentId) && Math.abs(avg - myAvg) <= 10;
                })
                .count();

        int peerLevel = scoreToLevel(myAvg);

        List<MaterialRecommendationDto> result = materialRepository
                .findByLevelLessThanEqualOrderByLevelDesc(peerLevel)
                .stream()
                .map(m -> MaterialRecommendationDto.builder()
                        .materialId(m.getMaterialId())
                        .title(m.getTitle())
                        .materialType(m.getMaterialType())
                        .matchReason(String.format(
                                "유사 성적대(%d점대) %d명의 학생이 선호하는 자료입니다.",
                                (int) Math.round(myAvg), peerCount))
                        .build())
                .collect(Collectors.toList());

        log.info("학생 {} 피어 콘텐츠 추천 완료 — 유사 학생 {}명, 추천 {}건", studentId, peerCount, result.size());
        return result;
    }

    // 동일 목표를 달성한 선배 사용자들의 성공 경로 추천
    public PeerSuccessPathDto recommendPeerSuccessPath(String studentId, String subject) {
        List<Grade> myGrades = gradeRepository.findByStudentIdAndSubjectOrderByExamDateDesc(studentId, subject);
        if (myGrades.isEmpty()) {
            return PeerSuccessPathDto.builder()
                    .subject(subject)
                    .similarStudentsCount(0)
                    .successMessage("성적 데이터가 없어 선배 경로를 분석할 수 없습니다.")
                    .keyInsights(List.of())
                    .build();
        }

        // 가장 오래된 점수 (DESC 정렬이므로 마지막 요소)
        double myFirstScore = myGrades.get(myGrades.size() - 1).getScore();

        // 해당 과목 전체 학생 성적 (날짜 오름차순)
        List<Grade> allSubjectGrades = gradeRepository.findBySubjectOrderByExamDateAsc(subject);
        Map<String, List<Grade>> byStudent = allSubjectGrades.stream()
                .collect(Collectors.groupingBy(Grade::getStudentId));

        // 시작 점수가 비슷하고(±15점) 10점 이상 향상된 "성공한 선배" 추출
        List<double[]> successData = byStudent.entrySet().stream()
                .filter(e -> !e.getKey().equals(studentId))
                .filter(e -> e.getValue().size() >= 2)
                .filter(e -> {
                    double first = e.getValue().get(0).getScore();
                    double last  = e.getValue().get(e.getValue().size() - 1).getScore();
                    return Math.abs(first - myFirstScore) <= 15 && (last - first) >= 10;
                })
                .map(e -> {
                    double first = e.getValue().get(0).getScore();
                    double last  = e.getValue().get(e.getValue().size() - 1).getScore();
                    return new double[]{first, last, last - first};
                })
                .collect(Collectors.toList());

        if (successData.isEmpty()) {
            return PeerSuccessPathDto.builder()
                    .subject(subject)
                    .similarStudentsCount(0)
                    .avgInitialScore(myFirstScore)
                    .successMessage("아직 유사 경로 데이터가 충분하지 않습니다. 더 많은 학생이 사용할수록 정확도가 높아집니다.")
                    .keyInsights(List.of(
                            "취약 개념을 먼저 파악하고 반복 학습하세요",
                            "오답 노트를 꾸준히 작성하세요",
                            "에빙하우스 복습 주기를 지켜 망각을 방지하세요"
                    ))
                    .build();
        }

        double avgFirst = successData.stream().mapToDouble(d -> d[0]).average().orElse(0);
        double avgLast  = successData.stream().mapToDouble(d -> d[1]).average().orElse(0);
        double avgImprovement = successData.stream().mapToDouble(d -> d[2]).average().orElse(0);

        return PeerSuccessPathDto.builder()
                .subject(subject)
                .similarStudentsCount(successData.size())
                .avgInitialScore(Math.round(avgFirst * 10) / 10.0)
                .avgFinalScore(Math.round(avgLast * 10) / 10.0)
                .avgImprovement(Math.round(avgImprovement * 10) / 10.0)
                .studyStrategyPattern(String.format(
                        "%s 과목에서 비슷한 시작점을 가진 선배 %d명이 평균 %.1f점 향상에 성공했습니다.",
                        subject, successData.size(), avgImprovement))
                .successMessage(String.format(
                        "시작 점수 %.0f점대에서 최종 %.0f점대까지 성장한 선배들의 경로입니다.",
                        avgFirst, avgLast))
                .keyInsights(List.of(
                        "취약 개념 집중 반복 학습이 공통 전략입니다",
                        "오답 유형을 분류하고 유형별로 집중 훈련했습니다",
                        "목표 점수를 먼저 정하고 역산하여 주간 학습량을 계획했습니다"
                ))
                .build();
    }

    private int scoreToLevel(double avgScore) {
        if (avgScore >= 85) return 5;
        if (avgScore >= 75) return 4;
        if (avgScore >= 65) return 3;
        if (avgScore >= 50) return 2;
        return 1;
    }

    public List<WrongAnswerEntity> recommendDailyReview(Long studentId) {
        LocalDate today = LocalDate.now();

        List<WrongAnswerEntity> dailyReview = wrongAnswerRepository
                .findByStudentIdOrderByFailCountDesc(studentId)
                .stream()
                .filter(w -> {
                    LocalDate last = w.getLastFailedDate();
                    return last != null && (
                            last.isEqual(today.minusDays(1)) ||
                            last.isEqual(today.minusDays(3)) ||
                            last.isEqual(today.minusDays(7))
                    );
                })
                .collect(Collectors.toList());

        log.info("학생 {} 오늘의 복습 대상 {}건 추출 완료 (에빙하우스 기준)", studentId, dailyReview.size());
        return dailyReview;
    }
}
