package com.i_route.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.ai.controller.MathAiController;
import com.i_route.backend.ai.entity.AiRecommendation;
import com.i_route.backend.ai.repository.AiRecommendationRepository;
import com.i_route.backend.ai.service.*;
import com.i_route.backend.global.config.*;
import com.i_route.backend.global.exception.GlobalExceptionHandler;
import com.i_route.backend.global.jwt.*;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 JWT/DB/컨트롤러를 사용하며 외부 AI 응답만 테스트 데이터로 대체한다. */
@SpringBootTest(classes = CounselingReportsIntegrationTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:counselingreports;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=counseling-test-signing-key-not-a-production-secret-12345678901234567890",
        "jwt.expiration=60000", "jwt.refresh-expiration=120000", "ai.server.key=local-test-key"})
@AutoConfigureMockMvc
class CounselingReportsIntegrationTest {
    @Configuration(proxyBeanMethods = false) @EnableAutoConfiguration
    @EntityScan("com.i_route.backend")
    @EnableJpaRepositories(basePackages = {"com.i_route.backend.ai.repository", "com.i_route.backend.user.repository",
            "com.i_route.backend.gps.domain.student.repository"})
    @Import({MathAiController.class, AiCounselingService.class, StudentRecordAccess.class, SecurityConfig.class,
            JwtAuthenticationFilter.class, AiServerKeyFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean AiStub ai() { return new AiStub(); }
        @Bean WebClient fastApiWebClient(AiStub ai) {
            return WebClient.builder().exchangeFunction(request -> {
                ai.calls++;
                return Mono.just(ClientResponse.create(ai.status)
                        .header("Content-Type", "application/json")
                        .body("{\"studentId\":" + ai.studentId + ",\"careerAnalysis\":\"분석 본문\",\"learningGuide\":\"학습 안내\"}")
                        .build());
            }).build();
        }
    }
    static class AiStub { volatile int calls; volatile Long studentId; volatile HttpStatus status; }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired AcademyRepository academies;
    @Autowired StudentRepository students;
    @Autowired AiRecommendationRepository reports;
    @Autowired JdbcTemplate jdbc;
    @Autowired AiStub ai;
    Long parentId, own, sameAcademy, otherAcademy;
    String parent, academy, noAcademy, driver, admin;

    private User user(User.UserRole role) {
        String name = UUID.randomUUID().toString();
        return users.save(User.builder().username(name).nickname(name).role(role).loginType(User.LoginType.EMAIL).build());
    }
    @BeforeEach void fixtures() {
        var p = user(User.UserRole.PARENT); var a = user(User.UserRole.ACADEMY);
        parentId = p.getId();
        var school = academies.save(Academy.builder().user(a).academyName("fixture-school").build());
        own = students.save(Student.builder().name("fixture").parentId(p.getId()).academyId(school.getAcademyId()).build()).getStudentId();
        sameAcademy = students.save(Student.builder().name("other-parent").parentId(-1L).academyId(school.getAcademyId()).build()).getStudentId();
        otherAcademy = students.save(Student.builder().name("other-school").parentId(-1L).academyId(-1L).build()).getStudentId();
        parent = jwt.generateToken(p.getId()); academy = jwt.generateToken(a.getId());
        noAcademy = jwt.generateToken(user(User.UserRole.ACADEMY).getId());
        driver = jwt.generateToken(user(User.UserRole.DRIVER).getId()); admin = jwt.generateToken(user(User.UserRole.ADMIN).getId());
        ai.calls = 0; ai.studentId = own; ai.status = HttpStatus.OK;
    }
    private String path(Long studentId) { return "/api/counseling/reports?studentId=" + studentId; }
    private AiRecommendation report(Long studentId, String title, String subject, LocalDateTime date) {
        return reports.save(AiRecommendation.builder().studentId(studentId).title(title).subject(subject)
                .careerAnalysis("진로 분석\n둘째 줄").learningGuide("학습 안내").createdAt(date).build());
    }
    private String read(Long studentId, String token) throws Exception {
        return mvc.perform(get(path(studentId)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsString();
    }
    private void denied(Long studentId, String token) throws Exception {
        mvc.perform(get(path(studentId)).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"error\":\"접근 권한이 없습니다.\"}"));
    }

    @Test void anonymousAndInvalidTokensCannotReadReports() throws Exception {
        mvc.perform(get(path(own))).andExpect(status().isUnauthorized());
        mvc.perform(head(path(own))).andExpect(status().isUnauthorized());
        for (String token : List.of("invalid", jwt.generateRefreshToken(parentId), jwt.generateToken(Long.MAX_VALUE))) {
            mvc.perform(get(path(own)).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
        assertThat(ai.calls).isZero();
    }

    @Test void returnsOnlyRequestedStudentWithStableLatestFirstOrderAndExactResponseFields() throws Exception {
        var time = LocalDateTime.of(2026, 11, 7, 10, 0);
        var old = report(own, "이전 리포트", "수학", time.minusDays(1));
        var first = report(own, "새 리포트", "영어", time);
        var second = report(own, "동시 생성 리포트", "과학", time);
        report(sameAcademy, "다른 학생 비공개 리포트", "국어", time.plusDays(1));
        var rows = json.readTree(read(own, parent));
        assertThat(rows).hasSize(3);
        assertThat(rows.findValuesAsText("id")).containsExactly(second.getId().toString(), first.getId().toString(), old.getId().toString());
        Set<String> fields = new HashSet<>(); rows.get(0).fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("id", "title", "subject", "careerAnalysis", "learningGuide", "createdAt");
        assertThat(rows.get(0).get("subject").asText()).isEqualTo("과학");
        assertThat(rows.get(0).get("careerAnalysis").asText()).isEqualTo("진로 분석\n둘째 줄");
        assertThat(rows.get(0).get("createdAt").asText()).isEqualTo("2026-11-07T10:00:00");
        assertThat(ai.calls).isZero();
    }

    @Test void parentAndAcademyCannotReadUnrelatedStudentsEvenWhenReportsAreEmpty() throws Exception {
        read(own, parent); denied(sameAcademy, parent); denied(otherAcademy, parent);
        read(own, academy); read(sameAcademy, academy); denied(otherAcademy, academy);
        denied(own, noAcademy); denied(own, driver); read(otherAcademy, admin);
        denied(Long.MAX_VALUE, parent); denied(Long.MAX_VALUE, academy);
        assertThat(ai.calls).isZero();
    }

    @Test void rejectsMissingMalformedAndNonPositiveStudentIds() throws Exception {
        for (String query : List.of("", "?studentId=", "?studentId=0", "?studentId=-1", "?studentId=abc",
                "?studentId=1.5", "?studentId=9223372036854775808")) {
            mvc.perform(get("/api/counseling/reports" + query).header("Authorization", "Bearer " + parent))
                    .andExpect(status().isBadRequest());
        }
        assertThat(ai.calls).isZero();
    }

    @Test void emptyHistoryHeadAndCorsWorkWithoutInvokingAi() throws Exception {
        assertThat(json.readTree(read(own, parent))).isEmpty();
        assertThat(read(own, parent)).isEqualTo(read(own, academy));
        mvc.perform(head(path(own)).header("Authorization", "Bearer " + parent)).andExpect(status().isOk());
        mvc.perform(options(path(own)).header("Origin", "https://d2nos5u98g310z.cloudfront.net")
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk());
        assertThat(ai.calls).isZero();
    }

    @Test void legacyRowsKeepNullsButKnownTitlesRecoverTheirSubjects() throws Exception {
        Map<String, String> titles = Map.of("수학 메타인지 분석 리포트", "수학", "인공지능 기반 진로 탐색 리포트", "국어",
                "영어 메타인지 분석 리포트", "영어", "과학 메타인지 분석 리포트", "과학",
                "사회 메타인지 분석 리포트", "사회", "i-Route 프리미엄 통합 리포트", "통합");
        titles.forEach((title, subject) -> report(own, title, null, null));
        var explicit = report(own, "수학 메타인지 분석 리포트", "사회", null);
        var unknown = reports.save(AiRecommendation.builder().studentId(own).build());
        assertThat(unknown.getCreatedAt()).isNotNull();
        jdbc.update("UPDATE ai_recommendation SET created_at = NULL WHERE id = ?", unknown.getId());
        var rows = json.readTree(read(own, parent));
        for (var row : rows) {
            if (row.get("id").asLong() == unknown.getId()) {
                assertThat(row.get("subject").isNull()).isTrue();
                assertThat(row.get("title").isNull()).isTrue();
                assertThat(row.get("createdAt").isNull()).isTrue();
            } else if (row.get("id").asLong() == explicit.getId()) {
                assertThat(row.get("subject").asText()).isEqualTo("사회");
            } else {
                assertThat(row.get("subject").asText()).isEqualTo(titles.get(row.get("title").asText()));
            }
        }
        assertThat(rows.get(rows.size() - 1).get("id").asLong()).isEqualTo(unknown.getId());
    }

    @Test void generatedReportsPersistSubjectAndAreVisibleAsSoonAsGenerationReturns() throws Exception {
        var subjects = Map.of("math", "수학", "writing", "국어", "premium", "통합", "영어", "영어", "과학", "과학", "사회", "사회");
        for (var entry : subjects.entrySet()) {
            mvc.perform(post("/api/counseling/" + entry.getKey()).param("studentId", own.toString())
                    .header("Authorization", "Bearer " + parent)).andExpect(status().isOk());
            var saved = reports.findByStudentIdOrderByCreatedAtDescIdDesc(own).get(0);
            assertThat(saved.getSubject()).isEqualTo(entry.getValue());
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getTitle()).isNotBlank();
            assertThat(json.readTree(read(own, academy)).get(0).get("id").asLong()).isEqualTo(saved.getId());
        }
        assertThat(json.readTree(read(own, parent))).hasSize(6);
        assertThat(ai.calls).isEqualTo(6);
    }

    @Test void generationCannotBypassStudentOwnership() throws Exception {
        for (String endpoint : List.of("math", "writing", "premium", "영어", "과학", "사회")) {
            mvc.perform(post("/api/counseling/" + endpoint).param("studentId", sameAcademy.toString())
                    .header("Authorization", "Bearer " + parent)).andExpect(status().isForbidden());
            mvc.perform(post("/api/counseling/" + endpoint).param("studentId", otherAcademy.toString())
                    .header("Authorization", "Bearer " + academy)).andExpect(status().isForbidden());
        }
        assertThat(ai.calls).isZero();
    }

    @Test void savedReportsRemainReadableWhileAiIsUnavailableAndFailedGenerationDoesNotSave() throws Exception {
        report(own, "저장된 리포트", "수학", null);
        ai.status = HttpStatus.SERVICE_UNAVAILABLE;
        assertThat(json.readTree(read(own, parent))).hasSize(1);
        mvc.perform(post("/api/counseling/math").param("studentId", own.toString())
                .header("Authorization", "Bearer " + parent)).andExpect(status().isServiceUnavailable());
        assertThat(json.readTree(read(own, parent))).hasSize(1);
        assertThat(ai.calls).isEqualTo(1);
    }
}
