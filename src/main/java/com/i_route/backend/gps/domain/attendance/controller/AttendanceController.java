package com.i_route.backend.gps.domain.attendance.controller;

import com.i_route.backend.gps.domain.attendance.dto.AttendanceResponse;
import com.i_route.backend.gps.domain.attendance.dto.AttendanceTagRequest;
import com.i_route.backend.gps.domain.attendance.dto.GradeStudentIdRequest;
import com.i_route.backend.gps.domain.attendance.dto.ManualAttendanceRequest;
import com.i_route.backend.gps.domain.attendance.dto.NfcPendingResponse;
import com.i_route.backend.gps.domain.attendance.dto.NfcRegisterQueueRequest;
import com.i_route.backend.gps.domain.attendance.dto.NfcRegisterRequest;
import com.i_route.backend.gps.domain.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * 기사 앱 → 수동 승하차 처리
     * POST /api/gps/attendance/manual
     * Body: { "studentId": 1, "eventType": "BOARD" }
     */
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/attendance/manual")
    public ResponseEntity<AttendanceResponse> manualAttendance(
            @Valid @RequestBody ManualAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.processManualAttendance(request));
    }

    /**
     * 학부모 앱 → 내 자녀 출결 이력 조회 (parentId 기반)
     * GET /api/gps/attendance/parents/{parentId}?date=2026-05-26
     */
    @GetMapping("/attendance/parents/{parentId}")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByParent(
            @PathVariable Long parentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(attendanceService.getAttendanceByParent(parentId, target));
    }

    /**
     * 학부모 앱 → 내 자녀 출결 이력 조회 (studentId 기반)
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
    @PreAuthorize("hasAnyRole('ACADEMY', 'ADMIN', 'DRIVER')")
    @GetMapping("/attendance/buses/{busId}")
    public ResponseEntity<List<AttendanceResponse>> getBusAttendance(
            @PathVariable Long busId) {
        return ResponseEntity.ok(attendanceService.getBusAttendanceToday(busId));
    }

    /**
     * 프론트 → NFC 카드 등록 요청을 큐에 추가
     * POST /api/gps/nfc/register-request
     * Body: { "studentId": 1 }
     */
    @PreAuthorize("hasAnyRole('ACADEMY', 'ADMIN', 'DRIVER')")
    @PostMapping("/nfc/register-request")
    public ResponseEntity<Void> enqueueNfcRegister(
            @Valid @RequestBody NfcRegisterQueueRequest request) {
        attendanceService.enqueueNfcRegister(request.getStudentId());
        return ResponseEntity.ok().build();
    }

    /**
     * 라즈베리파이 → 등록 대기 중인 학생 ID 조회 (폴링)
     * GET /api/gps/nfc/pending
     * 대기 없으면 204 No Content
     */
    @PreAuthorize("hasAnyRole('ACADEMY', 'ADMIN', 'DRIVER')")
    @GetMapping("/nfc/pending")
    public ResponseEntity<NfcPendingResponse> pollNfcPending() {
        return attendanceService.pollNfcRegisterQueue()
                .map(studentId -> ResponseEntity.ok(new NfcPendingResponse(studentId)))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * 관리자 → 학생에게 NFC 카드 등록 (PN532)
     * PATCH /api/gps/students/{studentId}/nfc
     */
    @PreAuthorize("hasAnyRole('ACADEMY', 'ADMIN', 'DRIVER')")
    @PatchMapping("/students/{studentId}/nfc")
    public ResponseEntity<Void> registerNfc(
            @PathVariable Long studentId,
            @Valid @RequestBody NfcRegisterRequest request) {
        attendanceService.registerNfcCard(studentId, request.getNfcCardId());
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자 → 학생에게 성적 시스템 ID 등록
     * PATCH /api/gps/students/{studentId}/grade-id
     */
    @PreAuthorize("hasAnyRole('ACADEMY', 'ADMIN', 'DRIVER')")
    @PatchMapping("/students/{studentId}/grade-id")
    public ResponseEntity<Void> registerGradeStudentId(
            @PathVariable Long studentId,
            @Valid @RequestBody GradeStudentIdRequest request) {
        attendanceService.registerGradeStudentId(studentId, request.getGradeStudentId());
        return ResponseEntity.ok().build();
    }
}
