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
@RequestMapping("/api/children")
@RequiredArgsConstructor
public class ChildrenController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<StudentResponse> addChild(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChildCreateRequest request) {
        StudentResponse response = studentService.addChild(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<StudentResponse>> getMyChildren(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<StudentResponse> children = studentService.getMyChildren(userDetails.getId());
        return ResponseEntity.ok(children);
    }
}
