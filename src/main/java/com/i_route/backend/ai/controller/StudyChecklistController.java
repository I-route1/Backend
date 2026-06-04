package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.StudyChecklistRequest;
import com.i_route.backend.ai.dto.StudyChecklistResponse;
import com.i_route.backend.ai.service.StudyChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/checklist")
@RequiredArgsConstructor
public class StudyChecklistController {

    private final StudyChecklistService checklistService;

    @GetMapping
    public ResponseEntity<List<StudyChecklistResponse>> getChecklist(
            @RequestParam Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(checklistService.getByDate(studentId, targetDate));
    }

    @PostMapping
    public ResponseEntity<StudyChecklistResponse> addItem(@RequestBody StudyChecklistRequest request) {
        return ResponseEntity.ok(checklistService.addItem(request));
    }

    @PatchMapping("/{itemId}/toggle")
    public ResponseEntity<StudyChecklistResponse> toggleDone(@PathVariable Long itemId) {
        return ResponseEntity.ok(checklistService.toggleDone(itemId));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        checklistService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
