package com.i_route.backend.gps.domain.attendance.controller;

import com.i_route.backend.gps.domain.attendance.dto.AttendanceResponse;
import com.i_route.backend.gps.domain.attendance.dto.AttendanceTagRequest;
import com.i_route.backend.gps.domain.attendance.dto.NfcRegisterRequest;
import com.i_route.backend.gps.domain.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/gps")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 라즈베리파이(PN532) → NFC 카드 태그 시 호출
     * POST /api/gps/attendance
     * Body: { "busId": 1, "nfcCardId": "A1B2C3D4" }
     */
    @PostMapping("/attendance")
    public ResponseEntity<AttendanceResponse> tagCard(
            @Valid @RequestBody AttendanceTagRequest request) {
        return ResponseEntity.ok(attendanceService.processTag(request));
    }

    /**
     * 학부모 앱 → 내 자녀 출결 이력 조회
     * GET /api/gps/attendance/students/{studentId}?date=2026-05-26
     */
    @GetMapping("/attendance/students/{studentId}")
    public ResponseEntity<List<AttendanceResponse>> getStudentHistory(
            @PathVariable Long studentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(attendanceService.getStudentHistory(studentId, target));
    }

    /**
     * 관리자/교사 → 오늘 버스 전체 출결 조회
     * GET /api/gps/attendance/buses/{busId}
     */
    @GetMapping("/attendance/buses/{busId}")
    public ResponseEntity<List<AttendanceResponse>> getBusAttendance(
            @PathVariable Long busId) {
        return ResponseEntity.ok(attendanceService.getBusAttendanceToday(busId));
    }

    /**
     * 관리자 → 학생에게 NFC 카드 등록 (PN532)
     * PATCH /api/gps/students/{studentId}/nfc
     * Body: { "nfcCardId": "A1B2C3D4" }
     */
    @PatchMapping("/students/{studentId}/nfc")
    public ResponseEntity<Void> registerNfc(
            @PathVariable Long studentId,
            @Valid @RequestBody NfcRegisterRequest request) {
        attendanceService.registerNfcCard(studentId, request.getNfcCardId());
        return ResponseEntity.ok().build();
    }
}
