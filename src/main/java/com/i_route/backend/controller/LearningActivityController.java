package com.i_route.backend.controller;

import com.i_route.backend.entity.LearningActivity;
import com.i_route.backend.repository.LearningActivityRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class LearningActivityController {

    private final LearningActivityRepository learningActivityRepository;

    // 1. 학생 자기 평가 (메타인지) 입력 API
    @PostMapping("/self-eval")
    public ResponseEntity<String> saveSelfEvaluation(@RequestBody SelfEvalRequest request) {
        LearningActivity activity = LearningActivity.builder()
                .studentId(request.getStudentId())
                .understandingScore(request.getUnderstandingScore())
                .focusScore(request.getFocusScore())
                .studentNote(request.getStudentNote())
                .build();

        learningActivityRepository.save(activity);
        return ResponseEntity.ok("자기 평가가 저장되었습니다. AI 리포트 분석에 활용됩니다.");
    }

    // 2. 강사 정성 피드백 입력 API
    @PostMapping("/teacher-feedback")
    public ResponseEntity<String> saveTeacherFeedback(@RequestBody TeacherFeedbackRequest request) {
        // 가장 최근의 학습 활동 기록을 찾아 피드백 업데이트 (실제로는 ID 기반 조회가 정석)
        LearningActivity activity = learningActivityRepository.findFirstByStudentIdOrderByCreatedAtDesc(request.getStudentId());

        if (activity != null) {
            // 편의상 새로운 엔티티로 저장하거나 기존 엔티티를 업데이트하는 로직이 들어갑니다.
            LearningActivity updatedActivity = LearningActivity.builder()
                    .studentId(activity.getStudentId())
                    .understandingScore(activity.getUnderstandingScore())
                    .focusScore(activity.getFocusScore())
                    .studentNote(activity.getStudentNote())
                    .teacherFeedback(request.getTeacherFeedback())
                    .teacherName(request.getTeacherName())
                    .build();

            learningActivityRepository.save(updatedActivity);
            return ResponseEntity.ok("강사 피드백이 성공적으로 등록되었습니다.");
        }

        return ResponseEntity.badRequest().body("피드백을 등록할 학생의 최신 학습 기록이 없습니다.");
    }

    @Data
    static class SelfEvalRequest {
        private String studentId;
        private int understandingScore;
        private int focusScore;
        private String studentNote;
    }

    @Data
    static class TeacherFeedbackRequest {
        private String studentId;
        private String teacherFeedback;
        private String teacherName;
    }
}