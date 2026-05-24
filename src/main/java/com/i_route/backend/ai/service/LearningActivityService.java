package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.LearningActivityRequest;
import com.i_route.backend.ai.dto.LearningActivityResponse;
import com.i_route.backend.ai.entity.LearningActivity;
import com.i_route.backend.ai.repository.LearningActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningActivityService {

    private final LearningActivityRepository learningActivityRepository;

    // 🌟 학습 기록 저장
    @Transactional
    public LearningActivityResponse saveActivity(LearningActivityRequest request) {
        LearningActivity activity = LearningActivity.builder()
                .studentId(request.getStudentId())
                .subject(request.getSubject())
                .studyDate(request.getStudyDate())
                .studyDurationMinutes(request.getStudyDurationMinutes())
                .understandingScore(request.getUnderstandingScore())
                .concentrationScore(request.getConcentrationScore())
                .instructorFeedback(request.getInstructorFeedback())
                .build();

        LearningActivity savedActivity = learningActivityRepository.save(activity);
        return LearningActivityResponse.from(savedActivity);
    }

    // 🌟 특정 학생의 전체 학습 기록 조회 (최신순)
    @Transactional(readOnly = true)
    public List<LearningActivityResponse> getActivitiesByStudent(String studentId) {
        return learningActivityRepository.findByStudentIdOrderByStudyDateDesc(studentId)
                .stream()
                .map(LearningActivityResponse::from)
                .collect(Collectors.toList());
    }
}