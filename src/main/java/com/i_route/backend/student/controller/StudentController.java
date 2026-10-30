package com.i_route.backend.student.controller;

import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.student.dto.ChildCreateRequest;
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

    // 학부모: 자녀 등록 / 수정 (ChildCreateRequest 기반)
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<StudentResponse> addStudent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChildCreateRequest request) {
        StudentResponse response = studentService.addChild(userDetails.getId(), request);
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

    // 💡 학부모: 자녀 삭제 (추가)
    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<String> deleteStudent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId) {
        studentService.deleteStudent(userDetails.getId(), studentId);
        return ResponseEntity.ok("자녀 정보가 정상적으로 삭제되었습니다.");
    }

    // 학원 관리자: 내 학원의 학생 목록 조회
    @GetMapping("/academy/students")
    @PreAuthorize("hasRole('ACADEMY')")
    public ResponseEntity<List<StudentResponse>> getAcademyStudents(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudentResponse> students = studentService.getAcademyStudents(userDetails.getId());
        return ResponseEntity.ok(students);
    }
}