package com.i_route.backend.gps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.global.config.*;
import com.i_route.backend.global.exception.GlobalExceptionHandler;
import com.i_route.backend.global.jwt.*;
import com.i_route.backend.gps.domain.attendance.controller.AttendanceController;
import com.i_route.backend.gps.domain.attendance.entity.*;
import com.i_route.backend.gps.domain.attendance.repository.*;
import com.i_route.backend.gps.domain.attendance.service.*;
import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.driver.entity.Driver;
import com.i_route.backend.gps.domain.driver.repository.DriverRepository;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.gps.domain.gps.controller.GpsController;
import com.i_route.backend.gps.domain.gps.service.GpsQueryService;
import com.i_route.backend.gps.domain.gps.service.GpsCommandService;
import com.i_route.backend.gps.domain.gps.service.EtaService;
import com.i_route.backend.gps.domain.gps.repository.CurrentLocationRedisRepository;
import com.i_route.backend.gps.domain.route.service.RouteProgressService;
import com.i_route.backend.user.entity.*;
import com.i_route.backend.user.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AttendanceAccessIntegrationTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:attendanceaccess;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=attendance-test-signing-key-not-a-production-secret-12345678901234567890",
        "jwt.expiration=60000", "jwt.refresh-expiration=120000", "ai.server.key=local-test-key"})
@AutoConfigureMockMvc
class AttendanceAccessIntegrationTest {
    @Configuration(proxyBeanMethods = false) @EnableAutoConfiguration
    @EntityScan("com.i_route.backend")
    @EnableJpaRepositories(basePackages = {"com.i_route.backend.gps.domain.attendance.repository",
            "com.i_route.backend.gps.domain.student.repository", "com.i_route.backend.gps.domain.bus.repository",
            "com.i_route.backend.gps.domain.driver.repository", "com.i_route.backend.gps.domain.route.repository",
            "com.i_route.backend.user.repository", "com.i_route.backend.board.repository"})
    @Import({AttendanceController.class, AttendanceService.class, AttendanceRecordAccess.class, SecurityConfig.class,
            JwtAuthenticationFilter.class, AiServerKeyFilter.class, JwtUtil.class, GlobalExceptionHandler.class,
            GpsController.class, GpsQueryService.class})
    static class Config {
        @Bean SimpMessagingTemplate messages() { return mock(SimpMessagingTemplate.class); }
        @Bean NfcRegisterQueueRedisRepository queue() { return mock(NfcRegisterQueueRedisRepository.class); }
        @Bean StudentBoardingRedisRepository boarding() { return mock(StudentBoardingRedisRepository.class); }
        @Bean GpsCommandService commands() { return mock(GpsCommandService.class); }
        @Bean CurrentLocationRedisRepository locations() { return mock(CurrentLocationRedisRepository.class); }
        @Bean RouteProgressService progress() { return mock(RouteProgressService.class); }
        @Bean EtaService eta() { return mock(EtaService.class); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired AcademyRepository academies;
    @Autowired StudentRepository students;
    @Autowired BusRepository buses;
    @Autowired DriverRepository drivers;
    @Autowired AttendanceRepository attendance;
    @Autowired StudentBoardingRedisRepository boarding;
    Long parentId, otherParentId, own, sameSchool, otherSchool, otherBusStudent, empty, busId, otherBusId;
    String parent, otherParent, academy, driver, unassigned, admin, noAcademy;
    LocalDate date;

    private User user(User.UserRole role) {
        var name = UUID.randomUUID().toString();
        return users.save(User.builder().username(name).nickname(name).role(role).loginType(User.LoginType.EMAIL).build());
    }
    private Long student(Long parentId, Long academyId, Long busId) {
        return students.save(Student.builder().name("private-student").parentId(parentId).academyId(academyId).busId(busId).build()).getStudentId();
    }
    private void record(Long studentId, Long busId, LocalDate day) {
        attendance.save(Attendance.builder().studentId(studentId).busId(busId).eventType(AttendanceEventType.BOARD)
                .timestamp(day.atTime(8, 10)).build());
    }
    @BeforeEach void fixtures() {
        date = LocalDate.now();
        var p = user(User.UserRole.PARENT); var p2 = user(User.UserRole.PARENT);
        var a = user(User.UserRole.ACADEMY); var d = user(User.UserRole.DRIVER);
        var school = academies.save(Academy.builder().academyName("own-school").user(a).build());
        var anotherSchool = academies.save(Academy.builder().academyName("other-school").user(user(User.UserRole.ACADEMY)).build());
        var assigned = drivers.save(Driver.builder().name("driver").userId(d.getId()).build());
        busId = buses.save(Bus.builder().busNumber("own").driver(assigned).build()).getId();
        otherBusId = buses.save(Bus.builder().busNumber("other").build()).getId();
        parentId = p.getId(); otherParentId = p2.getId();
        own = student(parentId, school.getAcademyId(), busId);
        sameSchool = student(otherParentId, school.getAcademyId(), busId);
        otherSchool = student(otherParentId, anotherSchool.getAcademyId(), busId);
        otherBusStudent = student(otherParentId, anotherSchool.getAcademyId(), otherBusId);
        empty = student(parentId, school.getAcademyId(), null);
        for (Long id : List.of(own, sameSchool, otherSchool)) record(id, busId, date);
        record(otherBusStudent, otherBusId, date); record(own, busId, date.minusDays(1));
        parent = jwt.generateToken(parentId); otherParent = jwt.generateToken(otherParentId);
        academy = jwt.generateToken(a.getId()); driver = jwt.generateToken(d.getId());
        unassigned = jwt.generateToken(user(User.UserRole.DRIVER).getId());
        admin = jwt.generateToken(user(User.UserRole.ADMIN).getId());
        noAcademy = jwt.generateToken(user(User.UserRole.ACADEMY).getId());
    }
    private String studentPath(Long id) { return "/api/gps/attendance/students/" + id + "?date=" + date; }
    private String parentPath(Long id) { return "/api/gps/attendance/parents/" + id + "?date=" + date; }
    private Set<Long> ids(String path, String token) throws Exception {
        var body = mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsString();
        Set<Long> ids = new HashSet<>();
        json.readTree(body).forEach(row -> ids.add(row.get("studentId").asLong()));
        return ids;
    }
    private void denied(String path, String token) throws Exception {
        mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden())
                .andExpect(content().json("{\"error\":\"접근 권한이 없습니다.\"}"));
    }
    @Test void anonymousAndInvalidTokensCannotReadAnyAttendanceRoute() throws Exception {
        for (String path : List.of(studentPath(own), parentPath(parentId), "/api/gps/attendance/buses/" + busId,
                "/api/gps/buses/" + busId + "/passengers/today")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(content().string(""));
            mvc.perform(head(path)).andExpect(status().isUnauthorized());
            for (String token : List.of("invalid", jwt.generateRefreshToken(parentId), jwt.generateToken(Long.MAX_VALUE))) {
                mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
            }
        }
    }
    @Test void parentCanReadOnlyOwnChildrenEvenWhenOtherChildSharesSchoolAndBus() throws Exception {
        assertThat(ids(studentPath(own), parent)).containsExactly(own);
        denied(studentPath(sameSchool), parent); denied(studentPath(otherSchool), parent);
        denied(studentPath(own), otherParent); denied(studentPath(Long.MAX_VALUE), parent);
        assertThat(ids(studentPath(empty), parent)).isEmpty();
    }
    @Test void parentIdAliasCannotBypassStudentOwnership() throws Exception {
        assertThat(ids(parentPath(parentId), parent)).containsExactly(own);
        denied(parentPath(otherParentId), parent); denied(parentPath(parentId), otherParent);
        denied(parentPath(parentId), academy); denied(parentPath(parentId), driver);
        assertThat(ids(parentPath(otherParentId), admin)).containsExactlyInAnyOrder(sameSchool, otherSchool, otherBusStudent);
    }
    @Test void academyReadsOnlyItsStudentsIncludingMixedAcademyBusRecords() throws Exception {
        assertThat(ids(studentPath(own), academy)).containsExactly(own);
        assertThat(ids(studentPath(sameSchool), academy)).containsExactly(sameSchool);
        denied(studentPath(otherSchool), academy); denied(studentPath(own), noAcademy);
        assertThat(ids("/api/gps/attendance/buses/" + busId, academy)).containsExactlyInAnyOrder(own, sameSchool);
        assertThat(ids("/api/gps/attendance/buses/" + otherBusId, academy)).isEmpty();
        denied("/api/gps/attendance/buses/" + busId, noAcademy);
    }
    @Test void driverReadsAssignedBusOnlyAndParentCannotUseBusAlias() throws Exception {
        assertThat(ids("/api/gps/attendance/buses/" + busId, driver)).containsExactlyInAnyOrder(own, sameSchool, otherSchool);
        assertThat(ids(studentPath(otherSchool), driver)).containsExactly(otherSchool);
        denied(studentPath(otherBusStudent), driver); denied(studentPath(empty), driver);
        denied(studentPath(own), unassigned); denied("/api/gps/attendance/buses/" + otherBusId, driver);
        denied("/api/gps/attendance/buses/" + busId, parent);
    }
    @Test void adminReadsRecordsAndDateFilteringReturnsOnlyRequestedDay() throws Exception {
        assertThat(ids(studentPath(otherSchool), admin)).containsExactly(otherSchool);
        assertThat(ids("/api/gps/attendance/buses/" + otherBusId, admin)).containsExactly(otherBusStudent);
        mvc.perform(get(studentPath(own).replace(date.toString(), date.minusDays(1).toString()))
                .header("Authorization", "Bearer " + parent)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(studentPath(own).replace(date.toString(), date.plusDays(1).toString()))
                .header("Authorization", "Bearer " + parent)).andExpect(status().isOk()).andExpect(content().json("[]"));
        denied(studentPath(sameSchool).replace(date.toString(), date.plusDays(1).toString()), parent);
    }
    @Test void malformedIdsAndDatesFailWithoutDisclosingRecords() throws Exception {
        for (String path : List.of("/api/gps/attendance/students/0", "/api/gps/attendance/students/-1",
                "/api/gps/attendance/students/abc", "/api/gps/attendance/students/1.5",
                "/api/gps/attendance/parents/0", "/api/gps/attendance/buses/0",
                studentPath(own).replace(date.toString(), "2026-02-30"))) {
            mvc.perform(get(path).header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
        }
        mvc.perform(options(studentPath(own)).header("Origin", "https://d2nos5u98g310z.cloudfront.net")
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk());
    }
    @Test void passengerListCannotBypassAttendanceOwnership() throws Exception {
        String path = "/api/gps/buses/" + busId + "/passengers/today";
        denied(path, parent); denied(path, unassigned); denied(path, noAcademy);
        assertThat(ids(path, academy)).containsExactlyInAnyOrder(own, sameSchool);
        assertThat(ids(path, driver)).containsExactlyInAnyOrder(own, sameSchool, otherSchool);
        assertThat(ids(path, admin)).containsExactlyInAnyOrder(own, sameSchool, otherSchool);
        denied("/api/gps/buses/" + otherBusId + "/passengers/today", driver);
    }

    private String uid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(Locale.ROOT);
    }

    private Student cardHolder(String uid) {
        return students.saveAndFlush(Student.builder().name("NFC 합성 학생").nfcCardId(uid)
                .busId(busId).parentId(parentId).academyId(students.findById(own).orElseThrow().getAcademyId())
                .routeStopId(4L).expectedDropOffTime(LocalTime.of(16, 30)).gradeStudentId("linked")
                .grade("중1").profileImage("/test.png").currentLevel(2).learningTendency(Student.LearningTendency.VISUAL)
                .currentKoreanGrade(80.0).studyTime(12.0).studentNote("test").recommendContext("test").build());
    }

    private ResultActions tag(Long bus, String uid, String scanId) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("busId", bus, "nfcCardId", uid));
        if (scanId != null) body.put("scanId", scanId);
        return mvc.perform(post("/api/gps/attendance").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private ResultActions register(Long student, String uid) throws Exception {
        return mvc.perform(patch("/api/gps/students/" + student + "/nfc").header("Authorization", "Bearer " + academy)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nfcCardId", uid))));
    }

    @Test void concurrentRetriesCommitOneBoardAndReplayItEvenAfterExit() throws Exception {
        var s = cardHolder(uid());
        String first = UUID.randomUUID().toString();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> requests = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                requests.add(pool.submit(() -> {
                    start.await();
                    return tag(busId, s.getNfcCardId(), first).andExpect(status().isOk())
                            .andExpect(jsonPath("$.eventType").value("BOARD")).andReturn().getResponse().getContentAsString();
                }));
            }
            start.countDown();
            String original = requests.get(0).get(10, TimeUnit.SECONDS);
            for (var response : requests) assertThat(response.get(10, TimeUnit.SECONDS)).isEqualTo(original);
            tag(busId, s.getNfcCardId(), UUID.randomUUID().toString()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventType").value("EXIT"));
            tag(busId, s.getNfcCardId(), first.toUpperCase(Locale.ROOT)).andExpect(status().isOk())
                    .andExpect(content().string(original));
            assertThat(attendance.findByStudentIdAndTimestampBetweenOrderByTimestampDesc(s.getStudentId(),
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay())).extracting(Attendance::getEventType)
                    .containsExactly(AttendanceEventType.EXIT, AttendanceEventType.BOARD);
            verify(boarding, times(1)).markBoarded(s.getStudentId(), busId);
            verify(boarding, times(1)).markExited(s.getStudentId());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test void legacyReaderShortRepeatReturnsOriginalRecord() throws Exception {
        var s = cardHolder(uid());
        String first = tag(busId, s.getNfcCardId(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("BOARD")).andReturn().getResponse().getContentAsString();
        tag(busId, s.getNfcCardId(), null).andExpect(status().isOk()).andExpect(content().string(first));
        verify(boarding, times(1)).markBoarded(s.getStudentId(), busId);
    }

    @Test void unknownOrWrongBusNeverRegistersAttendanceOrOffersCardRegistration() throws Exception {
        var s = cardHolder(uid());
        String scan = UUID.randomUUID().toString();
        long before = attendance.count();
        tag(Long.MAX_VALUE, s.getNfcCardId(), scan).andExpect(status().isUnprocessableEntity());
        tag(otherBusId, s.getNfcCardId(), scan).andExpect(status().isUnprocessableEntity());
        tag(Long.MAX_VALUE, uid(), scan).andExpect(status().isUnprocessableEntity());
        tag(0L, s.getNfcCardId(), scan).andExpect(status().isBadRequest());
        tag(busId, s.getNfcCardId(), "broken").andExpect(status().isBadRequest());
        tag(busId, uid(), scan).andExpect(status().isNotFound());
        mvc.perform(post("/api/gps/attendance/manual").header("Authorization", "Bearer " + driver)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("studentId", empty, "eventType", "BOARD"))))
                .andExpect(status().isUnprocessableEntity());
        assertThat(attendance.count()).isEqualTo(before);
    }

    @Test void cardReplacementKeepsAllOtherStudentDataAndDisablesPreviousCard() throws Exception {
        var s = cardHolder(uid());
        var another = cardHolder(uid());
        String replacement = uid();
        register(s.getStudentId(), replacement).andExpect(status().isOk());
        register(s.getStudentId(), replacement).andExpect(status().isOk());
        register(another.getStudentId(), replacement).andExpect(status().isConflict());
        var updated = students.findById(s.getStudentId()).orElseThrow();
        assertThat(updated.getNfcCardId()).isEqualTo(replacement);
        assertThat(updated).usingRecursiveComparison().ignoringFields("nfcCardId").isEqualTo(s);
        assertThat(students.findById(another.getStudentId()).orElseThrow().getNfcCardId()).isEqualTo(another.getNfcCardId());
        tag(busId, s.getNfcCardId(), null).andExpect(status().isNotFound());
        tag(busId, replacement, null).andExpect(status().isOk()).andExpect(jsonPath("$.eventType").value("BOARD"));
    }

    @Test void concurrentRegistrationOfOneCardHasOnlyOneOwner() throws Exception {
        var first = cardHolder(uid());
        var second = cardHolder(uid());
        String replacement = uid();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> registrations = new ArrayList<>();
            for (var student : List.of(first, second)) {
                registrations.add(pool.submit(() -> {
                    start.await();
                    return register(student.getStudentId(), replacement).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            assertThat(List.of(registrations.get(0).get(10, TimeUnit.SECONDS), registrations.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            var winner = students.findByNfcCardId(replacement).orElseThrow().getStudentId();
            var loser = winner.equals(first.getStudentId()) ? second : first;
            assertThat(students.findById(loser.getStudentId()).orElseThrow().getNfcCardId()).isEqualTo(loser.getNfcCardId());
        } finally {
            pool.shutdownNow();
        }
    }
}
