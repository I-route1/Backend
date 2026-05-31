package com.i_route.backend.gps.domain.attendance.service;

import com.i_route.backend.gps.domain.attendance.dto.AttendanceResponse;
import com.i_route.backend.gps.domain.attendance.dto.AttendanceTagRequest;
import com.i_route.backend.gps.domain.attendance.entity.Attendance;
import com.i_route.backend.gps.domain.attendance.entity.AttendanceEventType;
import com.i_route.backend.gps.domain.attendance.repository.AttendanceRepository;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 라즈베리파이(PN532)에서 NFC 태그 시 호출 — 승/하차 자동 판단 후 저장 + WebSocket 알림
     */
    @Transactional
    public AttendanceResponse processTag(AttendanceTagRequest request) {
        Student student = studentRepository.findByNfcCardId(request.getNfcCardId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "등록되지 않은 NFC 카드입니다: " + request.getNfcCardId()));

        // 마지막 기록 기준으로 승/하차 자동 결정
        AttendanceEventType eventType = determineEventType(student.getStudentId());

        Attendance attendance = Attendance.builder()
                .studentId(student.getStudentId())
                .busId(request.getBusId())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();

        attendance = attendanceRepository.save(attendance);
        log.info("[출결] 학생={} eventType={} busId={}", student.getName(), eventType, request.getBusId());

        AttendanceResponse response = AttendanceResponse.builder()
                .attendanceId(attendance.getId())
                .studentId(student.getStudentId())
                .studentName(student.getName())
                .busId(request.getBusId())
                .eventType(eventType)
                .timestamp(attendance.getTimestamp())
                .build();

        // 학부모에게 WebSocket 실시간 알림
        if (student.getParentId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/attendance/" + student.getParentId(), response);
            log.info("[WebSocket] 학부모 {}에게 출결 알림 전송", student.getParentId());
        }

        return response;
    }

    /**
     * 학생의 마지막 출결 기록을 보고 다음 이벤트 타입 결정
     * 마지막이 BOARD 또는 오늘 기록 없음 → EXIT / BOARD 판단
     */
    private AttendanceEventType determineEventType(Long studentId) {
        return attendanceRepository.findTopByStudentIdOrderByTimestampDesc(studentId)
                .filter(last -> last.getTimestamp().toLocalDate().equals(LocalDate.now()))
                .map(last -> last.getEventType() == AttendanceEventType.BOARD
                        ? AttendanceEventType.EXIT
                        : AttendanceEventType.BOARD)
                .orElse(AttendanceEventType.BOARD);
    }

    /**
     * 학부모: 특정 학생의 출결 이력 조회 (기본 오늘)
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getStudentHistory(Long studentId, LocalDate date) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);

        return attendanceRepository
                .findByStudentIdAndTimestampBetweenOrderByTimestampDesc(studentId, from, to)
                .stream()
                .map(a -> AttendanceResponse.builder()
                        .attendanceId(a.getId())
                        .studentId(a.getStudentId())
                        .studentName(student.getName())
                        .busId(a.getBusId())
                        .eventType(a.getEventType())
                        .timestamp(a.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 관리자/교사: 오늘 특정 버스의 전체 출결 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getBusAttendanceToday(Long busId) {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);

        return attendanceRepository
                .findByBusIdAndTimestampBetweenOrderByTimestampAsc(busId, from, to)
                .stream()
                .map(a -> {
                    String name = studentRepository.findById(a.getStudentId())
                            .map(Student::getName).orElse("미등록");
                    return AttendanceResponse.builder()
                            .attendanceId(a.getId())
                            .studentId(a.getStudentId())
                            .studentName(name)
                            .busId(a.getBusId())
                            .eventType(a.getEventType())
                            .timestamp(a.getTimestamp())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 관리자: 학생에게 NFC 카드 등록 (PN532)
     */
    @Transactional
    public void registerNfcCard(Long studentId, String nfcCardId) {
        // 이미 다른 학생에게 등록된 카드인지 확인
        studentRepository.findByNfcCardId(nfcCardId).ifPresent(existing -> {
            if (!existing.getStudentId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 학생에게 등록된 카드입니다.");
            }
        });

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));

        Student updated = Student.builder()
                .studentId(student.getStudentId())
                .busId(student.getBusId())
                .routeStopId(student.getRouteStopId())
                .name(student.getName())
                .expectedDropOffTime(student.getExpectedDropOffTime())
                .parentId(student.getParentId())
                .nfcCardId(nfcCardId)
                .build();

        studentRepository.save(updated);
        log.info("[NFC 등록] 학생={} cardId={}", student.getName(), nfcCardId);
    }
}
