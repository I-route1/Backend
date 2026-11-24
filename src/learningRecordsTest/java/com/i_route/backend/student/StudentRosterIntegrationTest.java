package com.i_route.backend.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.board.repository.BoardRepository;
import com.i_route.backend.board.service.BoardService;
import com.i_route.backend.global.config.AiServerKeyFilter;
import com.i_route.backend.global.config.SecurityConfig;
import com.i_route.backend.global.exception.GlobalExceptionHandler;
import com.i_route.backend.global.jwt.JwtAuthenticationFilter;
import com.i_route.backend.global.jwt.JwtUtil;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.student.controller.StudentController;
import com.i_route.backend.student.service.StudentService;
import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.user.entity.AcademyEntityListener;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.AcademyRepository;
import com.i_route.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StudentRosterIntegrationTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:studentroster;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=student-roster-test-signing-key-not-a-production-secret-12345678901234567890",
        "jwt.expiration=60000",
        "jwt.refresh-expiration=120000"
})
@AutoConfigureMockMvc
@Transactional
class StudentRosterIntegrationTest {
    private static final String ADMIN_ROSTER = "/api/students/admin/students";
    private static final String ACADEMY_ROSTER = "/api/students/academy/students";

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.i_route.backend")
    @EnableJpaRepositories(basePackageClasses = {BoardRepository.class, StudentRepository.class, UserRepository.class})
    @Import({StudentController.class, StudentService.class, AcademyEntityListener.class, SecurityConfig.class,
            JwtAuthenticationFilter.class, AiServerKeyFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
    static class Config {}

    @MockitoBean BoardService boardService;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired AcademyRepository academies;
    @Autowired StudentRepository students;

    private User administrator;
    private User academy;
    private User parent;
    private User driver;
    private Long registeredId;
    private Long unaffiliatedId;

    @BeforeEach
    void setUp() {
        administrator = createUser(User.UserRole.ADMIN);
        academy = createUser(User.UserRole.ACADEMY);
        parent = createUser(User.UserRole.PARENT);
        driver = createUser(User.UserRole.DRIVER);
        Academy owner = academies.save(Academy.builder().user(academy).academyName("시연 학원").build());
        registeredId = students.save(Student.builder().name("시연 학생").grade("초5")
                .gradeStudentId("demo-001").parentId(parent.getId()).academyId(owner.getAcademyId())
                .busId(91L).routeStopId(92L).nfcCardId("test-private-card").build()).getStudentId();
        unaffiliatedId = students.save(Student.builder().name("미연결 학생").build()).getStudentId();
    }

    @Test
    void administratorGetsOrderedRosterWithOnlyRequiredFields() throws Exception {
        var response = mvc.perform(get(ADMIN_ROSTER).header(HttpHeaders.AUTHORIZATION, token(administrator)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$[0].name").value("시연 학생"))
                .andExpect(jsonPath("$[0].grade").value("초5"))
                .andExpect(jsonPath("$[0].gradeStudentId").value("demo-001"))
                .andReturn().getResponse();
        var rows = json.readTree(response.getContentAsByteArray());
        assertThat(rows.findValuesAsText("studentId"))
                .containsExactly(registeredId.toString(), unaffiliatedId.toString());
        Set<String> fields = new HashSet<>();
        rows.get(0).fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("studentId", "name", "grade", "gradeStudentId");
        assertThat(rows.get(1).hasNonNull("grade")).isFalse();
    }

    @Test
    void nonAdministratorsCannotGetTheRoster() throws Exception {
        mvc.perform(get(ADMIN_ROSTER)).andExpect(status().isUnauthorized());
        for (User user : List.of(academy, parent, driver)) {
            mvc.perform(get(ADMIN_ROSTER).header(HttpHeaders.AUTHORIZATION, token(user)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void academyStillGetsOnlyItsOwnStudentsAndRejectsAdministrator() throws Exception {
        mvc.perform(get(ACADEMY_ROSTER).header(HttpHeaders.AUTHORIZATION, token(academy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(registeredId));
        mvc.perform(get(ACADEMY_ROSTER).header(HttpHeaders.AUTHORIZATION, token(administrator)))
                .andExpect(status().isForbidden());
    }

    private User createUser(User.UserRole role) {
        return users.save(User.builder().username("roster-" + role).nickname("roster-" + role)
                .role(role).loginType(User.LoginType.EMAIL).build());
    }

    private String token(User user) {
        return "Bearer " + jwt.generateToken(user.getId());
    }
}
