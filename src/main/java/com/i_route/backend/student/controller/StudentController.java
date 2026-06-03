package com.i_route.backend.student.controller;

import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.student.dto.StudentCreateRequest;
import com.i_route.backend.student.dto.StudentResponse;
import com.i_route.backend.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // 학부모: 자녀 등록
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<StudentResponse> addStudent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StudentCreateRequest request) {
        StudentResponse response = studentService.addStudent(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 학부모: 내 자녀 목록 조회
    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<StudentResponse>> getMyChildren(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudentResponse> children = studentService.getMyChildren(userDetails.getId());
        return ResponseEntity.ok(children);
    }
}
