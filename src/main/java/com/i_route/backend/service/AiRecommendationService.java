package com.i_route.backend.service;

import com.i_route.backend.dto.MaterialRecommendationDto;
import com.i_route.backend.dto.StudyMethodDto;
import com.i_route.backend.dto.StudyRoadmapDto;
import com.i_route.backend.entity.StudentEntity;
import com.i_route.backend.entity.StudyTendency;
import com.i_route.backend.entity.WrongAnswerEntity;
import com.i_route.backend.repository.StudentRepository;
import com.i_route.backend.repository.StudyMaterialRepository;
import com.i_route.backend.repository.TargetGoalRepository;
import com.i_route.backend.repository.WrongAnswerEntityRepository;
import com.i_route.backend.entity.TargetGoalEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final StudentRepository studentRepository;
    private final StudyMaterialRepository materialRepository;
    private final WrongAnswerEntityRepository wrongAnswerRepository;
    private final TargetGoalRepository targetGoalRepository;

    public List<MaterialRecommendationDto> recommendMaterials(Long studentId) {
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다: " + studentId));

        List<MaterialRecommendationDto> result = materialRepository
                .findByLevelLessThanEqualOrderByLevelDesc(student.getCurrentLevel())
                .stream()
                .map(m -> MaterialRecommendationDto.builder()
                        .materialId(m.getMaterialId())
                        .title(m.getTitle())
                        .materialType(m.getMaterialType())
                        .matchReason("현재 학습 레벨 " + student.getCurrentLevel() + "에 최적화된 자료입니다.")
                        .build())
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
                .orElseThrow(() -> new IllegalArgumentException("설정된 목표가 없습니다. 먼저 목표를 등록해주세요."));

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
