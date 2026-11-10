package com.i_route.backend.ai;

import com.fasterxml.jackson.databind.*;
import com.i_route.backend.ai.controller.PostureRecordController;
import com.i_route.backend.ai.repository.PostureRecordRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PostureRecordsIntegrationTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:posture;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=posture-test-key-not-a-production-secret-123456789012345678901234567890",
        "jwt.expiration=60000", "jwt.refresh-expiration=120000", "ai.server.key=local-test-key"})
@AutoConfigureMockMvc
class PostureRecordsIntegrationTest {
    @Configuration(proxyBeanMethods = false) @EnableAutoConfiguration @EntityScan("com.i_route.backend")
    @EnableJpaRepositories(basePackages = {"com.i_route.backend.ai.repository", "com.i_route.backend.user.repository",
            "com.i_route.backend.gps.domain.student.repository"})
    @Import({PostureRecordController.class, PostureRecordService.class, StudentRecordLock.class, StudentRecordAccess.class,
            SecurityConfig.class, JwtAuthenticationFilter.class, AiServerKeyFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
    static class Config { }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired AcademyRepository academies;
    @Autowired StudentRepository students;
    @Autowired PostureRecordRepository records;
    Long own, sameAcademy, foreign;
    String parent, academy, noAcademy, driver, admin;

    private User user(User.UserRole role) {
        String name = UUID.randomUUID().toString();
        return users.save(User.builder().username(name).nickname(name).role(role).loginType(User.LoginType.EMAIL).build());
    }
    @BeforeEach void fixtures() {
        var p = user(User.UserRole.PARENT); var a = user(User.UserRole.ACADEMY);
        var school = academies.save(Academy.builder().user(a).academyName("posture-fixture").build());
        own = students.save(Student.builder().name("fixture").parentId(p.getId()).academyId(school.getAcademyId()).build()).getStudentId();
        sameAcademy = students.save(Student.builder().name("same-school").parentId(-1L).academyId(school.getAcademyId()).build()).getStudentId();
        foreign = students.save(Student.builder().name("other-school").parentId(-1L).academyId(-1L).build()).getStudentId();
        parent = jwt.generateToken(p.getId()); academy = jwt.generateToken(a.getId());
        noAcademy = jwt.generateToken(user(User.UserRole.ACADEMY).getId());
        driver = jwt.generateToken(user(User.UserRole.DRIVER).getId()); admin = jwt.generateToken(user(User.UserRole.ADMIN).getId());
    }
    private Map<String, Object> body(Long student) {
        var body = new LinkedHashMap<String, Object>();
        body.put("studentId", student); body.put("submissionId", UUID.randomUUID().toString()); body.put("subject", "수학");
        body.put("startedAt", "2026-11-10T10:00:00.123456789+09:00"); body.put("durationSeconds", 20);
        body.put("inputSource", "VIDEO"); body.put("modelName", "yolov8s_pose_h8"); body.put("totalSamples", 10);
        body.put("uprightSamples", 4); body.put("slouchedSamples", 2); body.put("headDownSamples", 1); body.put("tiltedSamples", 1);
        body.put("unknownSamples", 1); body.put("noPersonSamples", 1); body.put("multiplePeopleSamples", 0);
        return body;
    }
    private JsonNode save(Map<String, Object> body, String token) throws Exception {
        return json.readTree(mvc.perform(post("/api/posture-records").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode read(Long student, String token) throws Exception {
        return json.readTree(mvc.perform(get("/api/posture-records").param("studentId", student.toString())
                        .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test void saves_real_summary_without_face_scores_and_both_roles_read_it() throws Exception {
        var saved = save(body(own), academy);
        assertThat(saved.path("measurementSource").asText()).isEqualTo("AI_HAT");
        assertThat(saved.path("inputSource").asText()).isEqualTo("VIDEO");
        assertThat(saved.path("classifiedSamples").asInt()).isEqualTo(8);
        assertThat(saved.path("uprightRatio").asDouble()).isEqualTo(0.5);
        assertThat(saved.has("concentrationScore")).isFalse();
        assertThat(read(own, parent).get(0)).isEqualTo(saved);
        assertThat(read(own, academy).get(0)).isEqualTo(saved);
    }

    @Test void same_submission_retries_and_equivalent_timezones_do_not_duplicate() throws Exception {
        var request = body(own); var first = save(request, academy);
        request.put("startedAt", "2026-11-10T01:00:00.123456789Z");
        var second = save(request, parent);
        assertThat(second).isEqualTo(first);
        assertThat(read(own, parent)).hasSize(1);
    }

    @Test void different_content_with_same_submission_is_conflict() throws Exception {
        var request = body(own); save(request, academy); request.put("durationSeconds", 21);
        mvc.perform(post("/api/posture-records").header("Authorization", "Bearer " + parent)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request))).andExpect(status().isConflict());
        assertThat(read(own, parent)).hasSize(1);
    }

    @Test void no_classifiable_person_has_null_ratios_instead_of_good_posture() throws Exception {
        var request = body(own);
        for (var field : List.of("uprightSamples", "slouchedSamples", "headDownSamples", "tiltedSamples", "unknownSamples")) request.put(field, 0);
        request.put("noPersonSamples", 5); request.put("multiplePeopleSamples", 5);
        var saved = save(request, parent);
        assertThat(saved.path("classifiedSamples").asInt()).isZero();
        for (var field : List.of("uprightRatio", "slouchedRatio", "headDownRatio", "tiltedRatio")) assertThat(saved.path(field).isNull()).isTrue();
    }

    @Test void no_authentication_cannot_read_or_write() throws Exception {
        mvc.perform(get("/api/posture-records").param("studentId", own.toString())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posture-records").header("X-AI-Key", "local-test-key")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body(own)))).andExpect(status().isUnauthorized());
    }

    @Test void unrelated_parent_academy_and_driver_cannot_access_records() throws Exception {
        for (String token : List.of(parent, academy, noAcademy, driver)) {
            mvc.perform(get("/api/posture-records").param("studentId", foreign.toString())
                    .header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(post("/api/posture-records").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body(foreign)))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/posture-records").param("studentId", "999999999")
                .header("Authorization", "Bearer " + parent)).andExpect(status().isForbidden());
    }

    @Test void academy_and_admin_can_read_their_allowed_students() throws Exception {
        save(body(sameAcademy), academy);
        assertThat(read(sameAcademy, academy)).hasSize(1);
        assertThat(read(sameAcademy, admin)).hasSize(1);
        mvc.perform(get("/api/posture-records").param("studentId", sameAcademy.toString())
                .header("Authorization", "Bearer " + parent)).andExpect(status().isForbidden());
    }

    @Test void malformed_counters_sources_ids_and_times_are_rejected_without_writes() throws Exception {
        var invalid = new ArrayList<Map<String, Object>>();
        invalid.add(Map.of("totalSamples", 9)); invalid.add(Map.of("uprightSamples", -1));
        invalid.add(Map.of("uprightSamples", 4.9)); invalid.add(Map.of("uprightSamples", "4"));
        invalid.add(Map.of("uprightSamples", true)); invalid.add(Map.of("totalSamples", 86401));
        invalid.add(Map.of("inputSource", 0)); invalid.add(Map.of("inputSource", "camera"));
        invalid.add(Map.of("durationSeconds", 0)); invalid.add(Map.of("durationSeconds", 86401));
        invalid.add(Map.of("studentId", 1.5)); invalid.add(Map.of("studentId", "1e2"));
        invalid.add(Map.of("startedAt", 1760000000)); invalid.add(Map.of("startedAt", "2026-11-10T10:00:00"));
        invalid.add(Map.of("startedAt", "+999999999-12-31T23:59:59-18:00"));
        invalid.add(Map.of("submissionId", "../../unsafe")); invalid.add(Map.of("subject", "   "));
        long before = records.count();
        for (var change : invalid) {
            var request = body(own); request.putAll(change);
            mvc.perform(post("/api/posture-records").header("Authorization", "Bearer " + parent)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request))).andExpect(status().isBadRequest());
        }
        for (var field : List.of("studentId", "startedAt", "uprightSamples", "inputSource", "durationSeconds")) {
            var request = body(own); request.remove(field);
            mvc.perform(post("/api/posture-records").header("Authorization", "Bearer " + parent)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request))).andExpect(status().isBadRequest());
        }
        assertThat(records.count()).isEqualTo(before);
    }

    @Test void newest_order_subject_filter_and_limit_are_respected() throws Exception {
        var first = body(own); first.put("startedAt", "2026-11-09T01:00:00Z"); save(first, parent);
        var second = body(own); second.put("subject", "국어"); var latest = save(second, parent);
        assertThat(read(own, parent).get(0).path("id")).isEqualTo(latest.path("id"));
        mvc.perform(get("/api/posture-records").param("studentId", own.toString()).param("subject", "수학")
                .param("limit", "1").header("Authorization", "Bearer " + parent))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].subject").value("수학"));
    }

    @Test void malformed_or_duplicate_query_parameters_are_bad_requests() throws Exception {
        for (String value : List.of("0", "-1", "1.0", "1e2", "+1", " 1", "１", "99999999999999999999999"))
            mvc.perform(get("/api/posture-records").param("studentId", value).header("Authorization", "Bearer " + parent)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/posture-records").param("studentId", own.toString(), foreign.toString())
                .header("Authorization", "Bearer " + parent)).andExpect(status().isBadRequest());
        for (String value : List.of("0", "101", "1.5", "", " 1"))
            mvc.perform(get("/api/posture-records").param("studentId", own.toString()).param("limit", value)
                    .header("Authorization", "Bearer " + parent)).andExpect(status().isBadRequest());
    }

    @Test void simultaneous_retries_create_one_record() throws Exception {
        var request = body(own);
        var a = CompletableFuture.supplyAsync(() -> uncheckedSave(request));
        var b = CompletableFuture.supplyAsync(() -> uncheckedSave(request));
        assertThat(a.get(15, TimeUnit.SECONDS).path("id")).isEqualTo(b.get(15, TimeUnit.SECONDS).path("id"));
        assertThat(read(own, parent)).hasSize(1);
    }
    private JsonNode uncheckedSave(Map<String, Object> request) {
        try { return save(request, parent); }
        catch (Exception error) { throw new CompletionException(error); }
    }
}
