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
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
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
            "com.i_route.backend.user.repository"})
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
}
