package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.StudyRoadmapDto;
import com.i_route.backend.ai.dto.TargetGoalDto;
import com.i_route.backend.ai.entity.TargetGoalEntity;
import com.i_route.backend.ai.repository.TargetGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyPlanningService {

    private final TargetGoalRepository targetGoalRepository;

    public StudyRoadmapDto designProgressPlan(Long studentId, TargetGoalDto goalDto) {
        LocalDate targetDate = goalDto.getTargetDate();
        long remainingDays = LocalDate.now().until(targetDate).getDays();
        int dailyHours = goalDto.getDailyStudyHours() != null ? goalDto.getDailyStudyHours() : 2;

        if (remainingDays <= 0) {
            throw new IllegalArgumentException("목표 날짜가 오늘보다 이전입니다.");
        }

        // 목표 정보를 DB에 저장 (기존 목표가 있으면 덮어쓰기)
        targetGoalRepository.findByStudentId(studentId).ifPresent(targetGoalRepository::delete);
        targetGoalRepository.save(TargetGoalEntity.builder()
                .studentId(studentId)
                .targetKeyword(goalDto.getTargetKeyword())
                .targetDate(targetDate)
                .dailyStudyHours(dailyHours)
                .build());

        long remainingWeeks = Math.max(remainingDays / 7, 1);
        long totalStudyHours = remainingDays * dailyHours;

        List<String> milestones = new ArrayList<>();
        long phases = Math.min(remainingWeeks, 8);
        for (int i = 1; i <= phases; i++) {
            long weeklyHours = (long) dailyHours * 7;
            if (i <= phases / 3) {
                milestones.add(i + "주차 (" + weeklyHours + "시간): 기초 개념 완성 및 취약 단원 집중 정리");
            } else if (i <= phases * 2 / 3) {
                milestones.add(i + "주차 (" + weeklyHours + "시간): 유형별 심화 문제 및 오답 반복 훈련");
            } else {
                milestones.add(i + "주차 (" + weeklyHours + "시간): 실전 모의 + 최종 약점 점검");
            }
        }

        log.info("학생 {} 진도 설계 완료 - 목표: {}, 잔여 {}일, 총 {}시간",
                studentId, goalDto.getTargetKeyword(), remainingDays, totalStudyHours);

        return StudyRoadmapDto.builder()
                .studentId(studentId)
                .targetKeyword(goalDto.getTargetKeyword())
                .targetDate(targetDate)
                .weeklyMilestones(milestones)
                .overallStrategy("하루 " + dailyHours + "시간 기준 총 " + totalStudyHours + "시간 투자 계획 (잔여 " + remainingDays + "일)")
                .build();
    }
}
