package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.OcrResultRequest;
import com.i_route.backend.ai.dto.OcrResultResponse;
import com.i_route.backend.ai.service.OcrResultService;
import com.i_route.backend.ai.service.StudentRecordAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ocr-results")
@RequiredArgsConstructor
@Validated
public class OcrResultController {
    private final OcrResultService service;
    private final StudentRecordAccess access;

    @PostMapping
    public OcrResultResponse save(@Valid @RequestBody OcrResultRequest request) {
        access.requireStudent(request.studentId());
        return service.save(request);
    }

    @GetMapping
    public List<OcrResultResponse> list(@RequestParam @Positive Long studentId,
                                        @RequestParam(required = false) String subject) {
        access.requireStudent(studentId);
        return service.getByStudent(studentId, subject);
    }

    @GetMapping("/{id}")
    public OcrResultResponse get(@PathVariable @Positive Long id, @RequestParam @Positive Long studentId) {
        access.requireStudent(studentId);
        return service.get(id, studentId);
    }
}
