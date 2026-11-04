package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.FeedbackRequest;
import com.i_route.backend.ai.dto.LearningActivityRequest;
import com.i_route.backend.ai.dto.LearningActivityResponse;
import com.i_route.backend.ai.entity.LearningActivity;
import com.i_route.backend.ai.entity.MeasurementSource;
import com.i_route.backend.ai.repository.LearningActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningActivityService {

    private final LearningActivityRepository learningActivityRepository;
    private final StudentRecordLock studentLock;

    // 🌟 학습 기록 저장
    @Transactional
    public LearningActivityResponse saveActivity(LearningActivityRequest request) {
        studentLock.lock(request.getStudentId());
        var source = request.getMeasurementSource() == null ? MeasurementSource.MANUAL : request.getMeasurementSource();
        if (request.getSubmissionId() != null) {
            var existing = learningActivityRepository.findByStudentIdAndSubmissionId(request.getStudentId(), request.getSubmissionId());
            if (existing.isPresent()) {
                var a = existing.get();
                if (!sameContent(a, request, source)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "submissionId가 다른 학습 기록에 이미 사용되었습니다.");
                }
                return LearningActivityResponse.from(a);
            }
        }
        // 저장 키가 없던 기존 화면/PWA의 재전송도 같은 학습 시각·내용이면 재사용한다.
        // 새 화면은 기존 무키 기록에 키를 연결하므로 배포 전후에도 중복되지 않는다.
        if (source == MeasurementSource.MANUAL && request.getStudyStartTime() != null) {
            var legacy = learningActivityRepository.findByStudentIdAndSubjectAndStudyDateAndStudyStartTimeOrderByIdDesc(
                    request.getStudentId(), request.getSubject(), request.getStudyDate(), request.getStudyStartTime()).stream()
                    .filter(a -> request.getSubmissionId() == null || a.getSubmissionId() == null)
                    .filter(a -> sameContent(a, request, source)).findFirst();
            if (legacy.isPresent()) {
                var a = legacy.get();
                if (request.getSubmissionId() != null) a.attachSubmissionId(request.getSubmissionId());
                return LearningActivityResponse.from(a);
            }
        }
        LearningActivity activity = LearningActivity.builder()
                .studentId(request.getStudentId())
                .submissionId(request.getSubmissionId())
                .subject(request.getSubject())
                .studyDate(request.getStudyDate())
                .studyStartTime(request.getStudyStartTime())
                .studyDurationMinutes(request.getStudyDurationMinutes())
                .understandingScore(request.getUnderstandingScore())
                .concentrationScore(request.getConcentrationScore())
                .focusedRatio(request.getFocusedRatio())
                .drowsyRatio(request.getDrowsyRatio())
                .measurementSource(source)
                .build();

        LearningActivity savedActivity = learningActivityRepository.save(activity);
        return LearningActivityResponse.from(savedActivity);
    }

    private boolean sameContent(LearningActivity a, LearningActivityRequest request, MeasurementSource source) {
        return Objects.equals(a.getSubject(), request.getSubject())
                && Objects.equals(a.getStudyDate(), request.getStudyDate())
                && Objects.equals(a.getStudyStartTime(), request.getStudyStartTime())
                && a.getStudyDurationMinutes() == request.getStudyDurationMinutes()
                && a.getUnderstandingScore() == request.getUnderstandingScore()
                && a.getConcentrationScore() == request.getConcentrationScore()
                && Objects.equals(a.getFocusedRatio(), request.getFocusedRatio())
                && Objects.equals(a.getDrowsyRatio(), request.getDrowsyRatio())
                && (a.getMeasurementSource() == null ? MeasurementSource.MANUAL : a.getMeasurementSource()) == source;
    }

    // 🌟 특정 학생의 전체 학습 기록 조회 (최신순)
    @Transactional(readOnly = true)
    public List<LearningActivityResponse> getActivitiesByStudent(Long studentId) {
        return learningActivityRepository.findByStudentIdOrderByStudyDateDescStudyStartTimeDescIdDesc(studentId)
                .stream()
                .map(LearningActivityResponse::from)
                .collect(Collectors.toList());
    }

    // 🌟 강사 피드백 수정
    @Transactional
    public LearningActivityResponse updateFeedback(Long activityId, FeedbackRequest request) {
        LearningActivity activity = learningActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학습 기록을 찾을 수 없습니다."));
        activity.updateFeedback(request.getInstructorFeedback());
        return LearningActivityResponse.from(activity);
    }
}
