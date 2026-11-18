package com.i_route.backend.gps.domain.attendance.service;

import com.i_route.backend.gps.domain.attendance.dto.AttendanceResponse;
import com.i_route.backend.gps.domain.attendance.dto.AttendanceTagRequest;
import com.i_route.backend.gps.domain.attendance.dto.ManualAttendanceRequest;
import com.i_route.backend.gps.domain.attendance.entity.Attendance;
import com.i_route.backend.gps.domain.attendance.entity.AttendanceEventType;
import com.i_route.backend.gps.domain.attendance.repository.AttendanceRepository;
import com.i_route.backend.gps.domain.attendance.repository.NfcRegisterQueueRedisRepository;
import com.i_route.backend.gps.domain.attendance.repository.StudentBoardingRedisRepository;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final int LEGACY_REPEAT_SECONDS = 10;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final BusRepository busRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NfcRegisterQueueRedisRepository nfcRegisterQueueRedisRepository;
    private final StudentBoardingRedisRepository studentBoardingRedisRepository;

    @Transactional
    public AttendanceResponse processTag(AttendanceTagRequest request) {
        // Read the student under a row lock before taking a transaction snapshot for attendance.
        // Both concurrent NFC requests and driver corrections then see the preceding commit.
        var cardOwner = studentRepository.findLockedByNfcCardId(request.getNfcCardId());
        Long busId = request.getBusId();
        requireExistingBus(busId);
        Student student = cardOwner.orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "등록되지 않은 NFC 카드입니다."));
        if (!busId.equals(student.getBusId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "학생에게 배정된 버스와 일치하지 않습니다.");
        }
        String scanId = request.getScanId();
        if (scanId != null) {
            scanId = scanId.toLowerCase(Locale.ROOT);
            var recorded = attendanceRepository.findByStudentIdAndScanId(student.getStudentId(), scanId);
            if (recorded.isPresent()) {
                if (!busId.equals(recorded.get().getBusId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 버스에서 처리한 태그입니다.");
                }
                return responseFor(recorded.get(), student);
            }
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        var last = attendanceRepository.findTopByStudentIdOrderByTimestampDescIdDesc(student.getStudentId())
                .filter(row -> row.getTimestamp().toLocalDate().equals(now.toLocalDate()));
        if (scanId == null && last.filter(row -> busId.equals(row.getBusId())
                && row.getTimestamp().isAfter(now.minusSeconds(LEGACY_REPEAT_SECONDS))).isPresent()) {
            return responseFor(last.get(), student);
        }
        AttendanceEventType eventType = last.map(row -> row.getEventType() == AttendanceEventType.BOARD
                ? AttendanceEventType.EXIT : AttendanceEventType.BOARD).orElse(AttendanceEventType.BOARD);

        Attendance attendance = Attendance.builder()
                .studentId(student.getStudentId())
                .busId(busId)
                .scanId(scanId)
                .eventType(eventType)
                .timestamp(now)
                .build();

        attendance = attendanceRepository.save(attendance);
        log.info("[출결] 학생={} eventType={} busId={}", student.getName(), eventType, request.getBusId());

        AttendanceResponse response = responseFor(attendance, student);

        // 탑승 시 GPS 공유 활성화, 하차 시 비활성화
        if (eventType == AttendanceEventType.BOARD) {
            studentBoardingRedisRepository.markBoarded(student.getStudentId(), request.getBusId());
        } else {
            studentBoardingRedisRepository.markExited(student.getStudentId());
        }

        // WebSocket 알림은 트랜잭션 커밋 이후에 전송
        if (student.getParentId() != null) {
            Long parentId = student.getParentId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend("/topic/attendance/" + parentId, response);
                    log.info("[WebSocket] 학부모 {}에게 출결 알림 전송", parentId);
                }
            });
        }

        return response;
    }

    @Transactional
    public AttendanceResponse processManualAttendance(ManualAttendanceRequest request) {
        Student student = studentRepository.findLockedByStudentId(request.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다: " + request.getStudentId()));

        Long busId = student.getBusId();
        requireExistingBus(busId);
        AttendanceEventType eventType = request.getEventType();

        Attendance attendance = Attendance.builder()
                .studentId(student.getStudentId())
                .busId(busId)
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();

        attendance = attendanceRepository.save(attendance);
        log.info("[수동 출결] 학생={} eventType={} busId={}", student.getName(), eventType, busId);

        AttendanceResponse response = responseFor(attendance, student);

        if (eventType == AttendanceEventType.BOARD) {
            studentBoardingRedisRepository.markBoarded(student.getStudentId(), busId);
        } else {
            studentBoardingRedisRepository.markExited(student.getStudentId());
        }

        if (student.getParentId() != null) {
            Long parentId = student.getParentId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend("/topic/attendance/" + parentId, response);
                    log.info("[WebSocket] 학부모 {}에게 출결 알림 전송", parentId);
                }
            });
        }

        return response;
    }

    private void requireExistingBus(Long busId) {
        if (busId == null || !busRepository.existsById(busId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "버스 배정 또는 버스 정보를 확인하세요.");
        }
    }

    private AttendanceResponse responseFor(Attendance attendance, Student student) {
        return AttendanceResponse.builder()
                .attendanceId(attendance.getId())
                .studentId(student.getStudentId())
                .studentName(student.getName())
                .busId(attendance.getBusId())
                .eventType(attendance.getEventType())
                .timestamp(attendance.getTimestamp())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByParent(Long parentId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);

        return studentRepository.findAll().stream()
                .filter(s -> parentId.equals(s.getParentId()))
                .flatMap(s -> attendanceRepository
                        .findByStudentIdAndTimestampBetweenOrderByTimestampDesc(s.getStudentId(), from, to)
                        .stream()
                        .map(a -> AttendanceResponse.builder()
                                .attendanceId(a.getId())
                                .studentId(a.getStudentId())
                                .studentName(s.getName())
                                .busId(a.getBusId())
                                .eventType(a.getEventType())
                                .timestamp(a.getTimestamp())
                                .build()))
                .collect(Collectors.toList());
    }

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

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getBusAttendanceToday(Long busId, Long academyScope) {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);

        return attendanceRepository
                .findByBusIdAndTimestampBetweenOrderByTimestampAsc(busId, from, to)
                .stream()
                .filter(a -> academyScope == null || studentRepository.findById(a.getStudentId())
                        .map(s -> academyScope.equals(s.getAcademyId())).orElse(false))
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

    @Transactional
    public void registerNfcCard(Long studentId, String nfcCardId) {
        var cardOwner = studentRepository.findByNfcCardId(nfcCardId);
        if (cardOwner.isPresent()) {
            if (!cardOwner.get().getStudentId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 학생에게 등록된 카드입니다.");
            }
            return;
        }

        try {
            if (studentRepository.updateNfcCardId(studentId, nfcCardId) == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다.");
            }
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMostSpecificCause() instanceof java.sql.SQLException sql
                    && (sql.getErrorCode() == 1062 || "23505".equals(sql.getSQLState()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 학생에게 등록된 카드입니다.", ex);
            }
            throw ex;
        }
        log.info("[NFC 등록] 학생 ID={}", studentId);
    }

    public void enqueueNfcRegister(Long studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));
        nfcRegisterQueueRedisRepository.enqueue(studentId);
        log.info("[NFC 등록 요청] 학생={}", studentId);
    }

    public Optional<Long> pollNfcRegisterQueue() {
        return nfcRegisterQueueRedisRepository.dequeue();
    }

    @Transactional
    public void registerGradeStudentId(Long studentId, String gradeStudentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));

        Student updated = Student.builder()
                .studentId(student.getStudentId())
                .busId(student.getBusId())
                .routeStopId(student.getRouteStopId())
                .name(student.getName())
                .expectedDropOffTime(student.getExpectedDropOffTime())
                .parentId(student.getParentId())
                .nfcCardId(student.getNfcCardId())
                .gradeStudentId(gradeStudentId)
                .build();

        studentRepository.save(updated);
        log.info("[성적 ID 등록] 학생={} gradeStudentId={}", student.getName(), gradeStudentId);
    }
}
