package com.i_route.backend.controller;

import com.i_route.backend.dto.LearningActivityRequest;
import com.i_route.backend.dto.LearningActivityResponse;
import com.i_route.backend.service.LearningActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class LearningActivityController {

    private final LearningActivityService learningActivityService;

    // 🌟 학습 기록 입력 (POST)
    @PostMapping
    public ResponseEntity<LearningActivityResponse> saveActivity(@RequestBody LearningActivityRequest request) {
        LearningActivityResponse response = learningActivityService.saveActivity(request);
        return ResponseEntity.ok(response);
    }

    // 🌟 학습 기록 조회 (GET)
    @GetMapping("/{studentId}")
    public ResponseEntity<List<LearningActivityResponse>> getActivities(@PathVariable String studentId) {
        List<LearningActivityResponse> responses = learningActivityService.getActivitiesByStudent(studentId);
        return ResponseEntity.ok(responses);
    }
}