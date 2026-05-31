package com.i_route.backend.gps;

import com.i_route.backend.gps.domain.attendance.dto.AttendanceResponse;
import com.i_route.backend.gps.domain.attendance.dto.AttendanceTagRequest;
import com.i_route.backend.gps.domain.attendance.entity.Attendance;
import com.i_route.backend.gps.domain.attendance.entity.AttendanceEventType;
import com.i_route.backend.gps.domain.attendance.repository.AttendanceRepository;
import com.i_route.backend.gps.domain.attendance.service.AttendanceService;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @InjectMocks
    private AttendanceService attendanceService;

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    // ── 헬퍼 ─────────────────────────────────────────────────────

    private Student student(Long id, String nfcCardId, Long parentId) {
        return Student.builder()
                .studentId(id)
                .busId(1L)
                .routeStopId(10L)
                .name("홍길동")
                .expectedDropOffTime(LocalTime.of(16, 0))
                .parentId(parentId)
                .nfcCardId(nfcCardId)
                .build();
    }

    private Attendance attendance(Long id, Long studentId, AttendanceEventType type, LocalDateTime time) {
        return Attendance.builder()
                .id(id)
                .studentId(studentId)
                .busId(1L)
                .eventType(type)
                .timestamp(time)
                .build();
    }

    private AttendanceTagRequest tagRequest(Long busId, String nfcCardId) {
        AttendanceTagRequest req = new AttendanceTagRequest();
        ReflectionTestUtils.setField(req, "busId", busId);
        ReflectionTestUtils.setField(req, "nfcCardId", nfcCardId);
        return req;
    }

    // ── processTag ───────────────────────────────────────────────

    @Test
    @DisplayName("NFC 태그 처리 성공 - 오늘 첫 태그는 BOARD")
    void processTag_firstTagToday_isBoard() {
        Student s = student(7L, "A1B2C3D4", 100L);
        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.of(s));
        given(attendanceRepository.findTopByStudentIdOrderByTimestampDesc(7L)).willReturn(Optional.empty());
        Attendance saved = attendance(1L, 7L, AttendanceEventType.BOARD, LocalDateTime.now());
        given(attendanceRepository.save(any(Attendance.class))).willReturn(saved);

        AttendanceResponse result = attendanceService.processTag(tagRequest(1L, "A1B2C3D4"));

        assertThat(result.getEventType()).isEqualTo(AttendanceEventType.BOARD);
        assertThat(result.getStudentId()).isEqualTo(7L);
        assertThat(result.getStudentName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("NFC 태그 처리 성공 - BOARD 이후 태그는 EXIT")
    void processTag_afterBoard_isExit() {
        Student s = student(7L, "A1B2C3D4", 100L);
        Attendance lastBoard = attendance(1L, 7L, AttendanceEventType.BOARD, LocalDateTime.now().minusHours(1));

        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.of(s));
        given(attendanceRepository.findTopByStudentIdOrderByTimestampDesc(7L)).willReturn(Optional.of(lastBoard));
        Attendance saved = attendance(2L, 7L, AttendanceEventType.EXIT, LocalDateTime.now());
        given(attendanceRepository.save(any(Attendance.class))).willReturn(saved);

        AttendanceResponse result = attendanceService.processTag(tagRequest(1L, "A1B2C3D4"));

        assertThat(result.getEventType()).isEqualTo(AttendanceEventType.EXIT);
    }

    @Test
    @DisplayName("NFC 태그 처리 성공 - EXIT 이후 태그는 BOARD")
    void processTag_afterExit_isBoard() {
        Student s = student(7L, "A1B2C3D4", 100L);
        Attendance lastExit = attendance(2L, 7L, AttendanceEventType.EXIT, LocalDateTime.now().minusHours(1));

        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.of(s));
        given(attendanceRepository.findTopByStudentIdOrderByTimestampDesc(7L)).willReturn(Optional.of(lastExit));
        Attendance saved = attendance(3L, 7L, AttendanceEventType.BOARD, LocalDateTime.now());
        given(attendanceRepository.save(any(Attendance.class))).willReturn(saved);

        AttendanceResponse result = attendanceService.processTag(tagRequest(1L, "A1B2C3D4"));

        assertThat(result.getEventType()).isEqualTo(AttendanceEventType.BOARD);
    }

    @Test
    @DisplayName("NFC 태그 처리 성공 - 학부모에게 WebSocket 알림 전송")
    void processTag_sendsWebSocketToParent() {
        Student s = student(7L, "A1B2C3D4", 100L);
        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.of(s));
        given(attendanceRepository.findTopByStudentIdOrderByTimestampDesc(7L)).willReturn(Optional.empty());
        Attendance saved = attendance(1L, 7L, AttendanceEventType.BOARD, LocalDateTime.now());
        given(attendanceRepository.save(any(Attendance.class))).willReturn(saved);

        attendanceService.processTag(tagRequest(1L, "A1B2C3D4"));

        then(messagingTemplate).should().convertAndSend(eq("/topic/attendance/100"), any(AttendanceResponse.class));
    }

    @Test
    @DisplayName("NFC 태그 처리 성공 - parentId 없으면 WebSocket 전송 안 함")
    void processTag_noParent_noWebSocket() {
        Student s = student(7L, "A1B2C3D4", null);
        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.of(s));
        given(attendanceRepository.findTopByStudentIdOrderByTimestampDesc(7L)).willReturn(Optional.empty());
        Attendance saved = attendance(1L, 7L, AttendanceEventType.BOARD, LocalDateTime.now());
        given(attendanceRepository.save(any(Attendance.class))).willReturn(saved);

        attendanceService.processTag(tagRequest(1L, "A1B2C3D4"));

        then(messagingTemplate).should(never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("NFC 태그 처리 실패 - 등록되지 않은 카드")
    void processTag_unknownCard_throws404() {
        given(studentRepository.findByNfcCardId("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.processTag(tagRequest(1L, "UNKNOWN")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("등록되지 않은 NFC 카드");
    }

    @Test
    @DisplayName("NFC 태그 처리 - 어제 BOARD 기록은 무시하고 오늘 첫 태그로 BOARD 처리")
    void processTag_yesterdayRecord_treatedAsFirstToday() {
        Student s = student(7L, "A1B2C3D4", null);
        Attendance yesterday = attendance(1L, 7L, AttendanceEventType.BOARD,
                LocalDate.now().minusDays(1).atTime(8, 0));

        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.of(s));
        given(attendanceRepository.findTopByStudentIdOrderByTimestampDesc(7L)).willReturn(Optional.of(yesterday));
        Attendance saved = attendance(2L, 7L, AttendanceEventType.BOARD, LocalDateTime.now());
        given(attendanceRepository.save(any(Attendance.class))).willReturn(saved);

        AttendanceResponse result = attendanceService.processTag(tagRequest(1L, "A1B2C3D4"));

        assertThat(result.getEventType()).isEqualTo(AttendanceEventType.BOARD);
    }

    // ── getStudentHistory ────────────────────────────────────────

    @Test
    @DisplayName("학생 출결 이력 조회 성공")
    void getStudentHistory_success() {
        Student s = student(7L, "A1B2C3D4", null);
        LocalDate today = LocalDate.now();
        List<Attendance> records = List.of(
                attendance(1L, 7L, AttendanceEventType.BOARD, today.atTime(8, 30)),
                attendance(2L, 7L, AttendanceEventType.EXIT, today.atTime(16, 10))
        );

        given(studentRepository.findById(7L)).willReturn(Optional.of(s));
        given(attendanceRepository.findByStudentIdAndTimestampBetweenOrderByTimestampDesc(
                eq(7L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(records);

        List<AttendanceResponse> result = attendanceService.getStudentHistory(7L, today);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEventType()).isEqualTo(AttendanceEventType.BOARD);
        assertThat(result.get(1).getEventType()).isEqualTo(AttendanceEventType.EXIT);
    }

    @Test
    @DisplayName("학생 출결 이력 조회 실패 - 학생 없음")
    void getStudentHistory_studentNotFound() {
        given(studentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.getStudentHistory(99L, LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("학생을 찾을 수 없습니다");
    }

    // ── getBusAttendanceToday ────────────────────────────────────

    @Test
    @DisplayName("버스 전체 출결 조회 성공")
    void getBusAttendanceToday_success() {
        Student s = student(7L, "A1B2C3D4", null);
        List<Attendance> records = List.of(
                attendance(1L, 7L, AttendanceEventType.BOARD, LocalDateTime.now().minusHours(3))
        );

        given(attendanceRepository.findByBusIdAndTimestampBetweenOrderByTimestampAsc(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(records);
        given(studentRepository.findById(7L)).willReturn(Optional.of(s));

        List<AttendanceResponse> result = attendanceService.getBusAttendanceToday(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("버스 전체 출결 조회 - 미등록 학생은 이름 '미등록' 으로 반환")
    void getBusAttendanceToday_unknownStudent_showsMiDeungRok() {
        List<Attendance> records = List.of(
                attendance(1L, 999L, AttendanceEventType.BOARD, LocalDateTime.now())
        );

        given(attendanceRepository.findByBusIdAndTimestampBetweenOrderByTimestampAsc(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(records);
        given(studentRepository.findById(999L)).willReturn(Optional.empty());

        List<AttendanceResponse> result = attendanceService.getBusAttendanceToday(1L);

        assertThat(result.get(0).getStudentName()).isEqualTo("미등록");
    }

    // ── registerNfcCard ──────────────────────────────────────────

    @Test
    @DisplayName("NFC 카드 등록 성공")
    void registerNfcCard_success() {
        Student s = student(7L, null, null);
        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.empty());
        given(studentRepository.findById(7L)).willReturn(Optional.of(s));

        attendanceService.registerNfcCard(7L, "A1B2C3D4");

        then(studentRepository).should().save(any(Student.class));
    }

    @Test
    @DisplayName("NFC 카드 등록 실패 - 학생 없음")
    void registerNfcCard_studentNotFound() {
        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.empty());
        given(studentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.registerNfcCard(99L, "A1B2C3D4"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("학생을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("NFC 카드 등록 실패 - 다른 학생에게 이미 등록된 카드")
    void registerNfcCard_cardAlreadyRegisteredToOther() {
        Student other = student(99L, "A1B2C3D4", null);
        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.of(other));

        assertThatThrownBy(() -> attendanceService.registerNfcCard(7L, "A1B2C3D4"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 다른 학생에게 등록된 카드");
    }

    @Test
    @DisplayName("NFC 카드 등록 성공 - 본인 카드 재등록은 허용")
    void registerNfcCard_sameStudentReRegister_allowed() {
        Student s = student(7L, "A1B2C3D4", null);
        given(studentRepository.findByNfcCardId("A1B2C3D4")).willReturn(Optional.of(s));
        given(studentRepository.findById(7L)).willReturn(Optional.of(s));

        assertThatCode(() -> attendanceService.registerNfcCard(7L, "A1B2C3D4"))
                .doesNotThrowAnyException();
    }
}
