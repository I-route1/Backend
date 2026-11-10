package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.PostureRecordRequest;
import com.i_route.backend.ai.dto.PostureRecordResponse;
import com.i_route.backend.ai.service.PostureRecordService;
import com.i_route.backend.ai.service.StudentRecordAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/posture-records") @RequiredArgsConstructor
public class PostureRecordController {
    private final PostureRecordService service;
    private final StudentRecordAccess access;

    @PostMapping
    public PostureRecordResponse save(@Valid @RequestBody PostureRecordRequest request) {
        access.requireStudent(request.getStudentId());
        return service.save(request);
    }

    @GetMapping
    public List<PostureRecordResponse> list(@RequestParam Long studentId, @RequestParam(required = false) String subject,
                                            @RequestParam(defaultValue = "50") Integer limit, HttpServletRequest request) {
        requireSingleDecimal(request, "studentId", true);
        requireSingleDecimal(request, "limit", false);
        if (studentId <= 0 || limit < 1 || limit > 100) throw new IllegalArgumentException("학생 ID와 조회 개수를 확인해주세요.");
        if (subject != null && (request.getParameterValues("subject").length != 1 || subject.isBlank() || subject.length() > 255))
            throw new IllegalArgumentException("과목은 공백이 아닌 문자열 하나여야 합니다.");
        access.requireStudent(studentId);
        return service.list(studentId, subject == null ? null : subject.strip(), limit);
    }

    private void requireSingleDecimal(HttpServletRequest request, String name, boolean required) {
        String[] values = request.getParameterValues(name);
        if (values == null && !required) return;
        if (values == null || values.length != 1 || !values[0].matches("[0-9]+"))
            throw new IllegalArgumentException(name + "는 십진수 정수 하나여야 합니다.");
    }
}
