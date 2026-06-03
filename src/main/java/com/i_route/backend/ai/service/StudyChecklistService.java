package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.StudyChecklistRequest;
import com.i_route.backend.ai.dto.StudyChecklistResponse;
import com.i_route.backend.ai.entity.StudyChecklistItem;
import com.i_route.backend.ai.repository.StudyChecklistRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyChecklistService {

    private final StudyChecklistRepository checklistRepository;

    @Transactional(readOnly = true)
    public List<StudyChecklistResponse> getByDate(String studentId, LocalDate date) {
        return checklistRepository.findByStudentIdAndPlanDateOrderByIdAsc(studentId, date)
                .stream()
                .map(StudyChecklistResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudyChecklistResponse addItem(StudyChecklistRequest request) {
        LocalDate planDate = request.getPlanDate() != null ? request.getPlanDate() : LocalDate.now();
        StudyChecklistItem item = StudyChecklistItem.builder()
                .studentId(request.getStudentId())
                .subject(request.getSubject())
                .task(request.getTask())
                .estimatedMinutes(request.getEstimatedMinutes())
                .planDate(planDate)
                .build();
        return StudyChecklistResponse.from(checklistRepository.save(item));
    }

    @Transactional
    public StudyChecklistResponse toggleDone(Long itemId) {
        StudyChecklistItem item = checklistRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("항목을 찾을 수 없습니다."));
        item.toggleDone();
        return StudyChecklistResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        if (!checklistRepository.existsById(itemId)) {
            throw new EntityNotFoundException("항목을 찾을 수 없습니다.");
        }
        checklistRepository.deleteById(itemId);
    }
}
