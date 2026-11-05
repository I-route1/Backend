package com.i_route.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.ai.controller.*;
import com.i_route.backend.ai.dto.*;
import com.i_route.backend.ai.repository.*;
import com.i_route.backend.ai.service.*;
import com.i_route.backend.global.config.*;
import com.i_route.backend.global.jwt.*;
import com.i_route.backend.global.exception.GlobalExceptionHandler;
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
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = LearningRecordsEdgeTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:learningedges;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=local-test-signing-key-only-not-a-real-secret-12345678901234567890",
        "jwt.expiration=60000", "jwt.refresh-expiration=120000", "ai.server.key=local-test-key"})
@AutoConfigureMockMvc
class LearningRecordsEdgeTest {
    @Configuration @EnableAutoConfiguration
    @EntityScan("com.i_route.backend")
    @EnableJpaRepositories(basePackages = {"com.i_route.backend.ai.repository", "com.i_route.backend.user.repository",
            "com.i_route.backend.gps.domain.student.repository"})
    @Import({OcrResultController.class, WrongAnswerController.class, LearningActivityController.class,
            OcrResultService.class, WrongAnswerService.class, LearningActivityService.class,
            StudentRecordAccess.class, StudentRecordLock.class, SecurityConfig.class, JwtAuthenticationFilter.class,
            AiServerKeyFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
    static class Config {}
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @Autowired UserRepository users;
    @Autowired AcademyRepository academies;
    @Autowired StudentRepository students;
    @Autowired OcrResultService ocr;
    @Autowired LearningActivityService activities;
    @Autowired WrongAnswerService wrong;
    Long own, foreign, otherAcademy;
    String parent, academy, driver, admin;

    private User user(User.UserRole role) {
        var id = UUID.randomUUID().toString();
        return users.save(User.builder().username(id).nickname(id).role(role).loginType(User.LoginType.EMAIL).build());
    }
    @BeforeEach void fixtures() {
        var p = user(User.UserRole.PARENT); var a = user(User.UserRole.ACADEMY);
        var school = academies.save(Academy.builder().user(a).academyName("fixture").build());
        own = students.save(Student.builder().name("fixture").parentId(p.getId()).academyId(school.getAcademyId()).build()).getStudentId();
        foreign = students.save(Student.builder().name("other-parent").parentId(-1L).academyId(school.getAcademyId()).build()).getStudentId();
        otherAcademy = students.save(Student.builder().name("other-academy").parentId(-1L).academyId(-1L).build()).getStudentId();
        parent = jwt.generateToken(p.getId()); academy = jwt.generateToken(a.getId());
        driver = jwt.generateToken(user(User.UserRole.DRIVER).getId()); admin = jwt.generateToken(user(User.UserRole.ADMIN).getId());
    }
    private OcrResultRequest scan(String submission, String subject, int count, int length) {
        return new OcrResultRequest(own, subject, submission, count, 0,
                IntStream.range(0, count).mapToObj(i -> new OcrResultRequest.Question("Q" + i, "한".repeat(length),
                        "", 0.0, false, "받침", null)).toList());
    }
    private LearningActivityRequest evaluation(String submission) {
        var r = new LearningActivityRequest(); r.setStudentId(own); r.setSubmissionId(submission);
        r.setSubject("국어"); r.setStudyDate(LocalDate.of(2026,11,5)); r.setStudyStartTime(LocalTime.of(10,17));
        r.setStudyDurationMinutes(8); r.setUnderstandingScore(5); r.setConcentrationScore(1); return r;
    }
    @Test void enforcesParentAcademyDriverAndAdminOwnershipAcrossReadsAndWrites() throws Exception {
        for (String path : List.of("/api/activities/", "/api/wrong-answer/", "/api/ocr-results?studentId=")) {
            mvc.perform(get(path + own).header("Authorization", "Bearer " + parent)).andExpect(status().isOk());
            mvc.perform(get(path + foreign).header("Authorization", "Bearer " + parent)).andExpect(status().isForbidden());
            mvc.perform(get(path + foreign).header("Authorization", "Bearer " + academy)).andExpect(status().isOk());
            mvc.perform(get(path + otherAcademy).header("Authorization", "Bearer " + academy)).andExpect(status().isForbidden());
            mvc.perform(get(path + own).header("Authorization", "Bearer " + driver)).andExpect(status().isForbidden());
            mvc.perform(get(path + otherAcademy).header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        }
        var r = evaluation("forbidden"); r.setStudentId(foreign);
        mvc.perform(post("/api/activities").header("Authorization", "Bearer " + parent).contentType("application/json")
                .content(json.writeValueAsString(r))).andExpect(status().isForbidden());
        var s = scan("forbidden", "국어", 1, 1);
        var unrelated = new OcrResultRequest(foreign, s.subject(), s.submissionId(), 1, 0, s.questions());
        mvc.perform(post("/api/ocr-results").header("Authorization", "Bearer " + parent).contentType("application/json")
                .content(json.writeValueAsString(unrelated))).andExpect(status().isForbidden());
        assertThat(activities.getActivitiesByStudent(foreign)).isEmpty();
        assertThat(ocr.getByStudent(foreign, null)).isEmpty();
        mvc.perform(post("/api/wrong-answer/record").header("Authorization", "Bearer " + parent)
                .param("studentId", foreign.toString()).param("subject", "국어").param("questionId", "Q1")
                .param("conceptTag", "받침")).andExpect(status().isForbidden());
        var saved = ocr.save(scan("owned", "국어", 1, 1));
        mvc.perform(get("/api/ocr-results/" + saved.id() + "?studentId=" + foreign)
                .header("Authorization", "Bearer " + parent)).andExpect(status().isForbidden());
        mvc.perform(get("/api/ocr-results/" + saved.id() + "?studentId=" + foreign)
                .header("Authorization", "Bearer " + academy)).andExpect(status().isNotFound());
    }
    @Test void academyCanSaveOwnedStudentEvaluationAndParentReadsTheSameRecord() throws Exception {
        var request = evaluation("academy-evaluation");
        var body = json.writeValueAsString(request);
        var content = mvc.perform(post("/api/activities").header("Authorization", "Bearer " + academy)
                .contentType("application/json").content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("국어"))
                .andExpect(jsonPath("$.measurementSource").value("MANUAL"))
                .andExpect(jsonPath("$.understandingScore").value(5))
                .andExpect(jsonPath("$.concentrationScore").value(1))
                .andReturn().getResponse().getContentAsString();
        var id = json.readTree(content).get("id").asLong();
        mvc.perform(post("/api/activities").header("Authorization", "Bearer " + academy)
                .contentType("application/json").content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/activities/" + own).header("Authorization", "Bearer " + parent))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        request.setStudentId(otherAcademy);
        mvc.perform(post("/api/activities").header("Authorization", "Bearer " + academy)
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        assertThat(activities.getActivitiesByStudent(otherAcademy)).isEmpty();
    }
    @Test void academyCalendarBoundariesPersistAndUnsupportedYearsDoNotWrite() throws Exception {
        int index = 0;
        for (String date : List.of("1000-01-01", "2000-02-29", "2024-02-29", "9999-12-31")) {
            var r = evaluation("calendar-" + index++);
            r.setStudyDate(LocalDate.parse(date)); r.setStudyStartTime(LocalTime.of(23,59));
            r.setStudyDurationMinutes(index % 2 == 0 ? 1440 : 1);
            mvc.perform(post("/api/activities").header("Authorization", "Bearer " + academy)
                    .contentType("application/json").content(json.writeValueAsString(r)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.studyDate").value(date))
                    .andExpect(jsonPath("$.studyStartTime").value("23:59:00"));
        }
        var body = json.writeValueAsString(evaluation("invalid-calendar"));
        for (String date : List.of("0001-01-01", "0999-12-31", "10000-01-01", "1900-02-29", "2026-02-29")) {
            mvc.perform(post("/api/activities").header("Authorization", "Bearer " + academy)
                    .contentType("application/json").content(body.replace("2026-11-05", date)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(activities.getActivitiesByStudent(own)).hasSize(4);
    }
    @Test void concurrentParentAndAcademySubmissionsShareOneRecordButSeparateStudentsAndContent() throws Exception {
        var body = json.writeValueAsString(evaluation("cross-role"));
        var counter = new java.util.concurrent.atomic.AtomicInteger();
        var ids = concurrent(() -> {
            var token = counter.getAndIncrement() % 2 == 0 ? academy : parent;
            var response = mvc.perform(post("/api/activities").header("Authorization", "Bearer " + token)
                    .contentType("application/json").content(body)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return json.readTree(response).get("id").asLong();
        });
        assertThat(ids).containsOnly(ids.get(0));
        var r = evaluation("cross-role"); r.setStudentId(foreign);
        var other = mvc.perform(post("/api/activities").header("Authorization", "Bearer " + academy)
                .contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(other).get("id").asLong()).isNotEqualTo(ids.get(0));
        r.setStudentId(own); r.setUnderstandingScore(2);
        mvc.perform(post("/api/activities").header("Authorization", "Bearer " + academy)
                .contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isConflict());
        r.setSubmissionId("changed-content");
        mvc.perform(post("/api/activities").header("Authorization", "Bearer " + academy)
                .contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk());
        assertThat(activities.getActivitiesByStudent(own)).hasSize(2);
        assertThat(activities.getActivitiesByStudent(foreign)).hasSize(1);
    }
    @Test void requiresAccessTokenAndRejectsMissingMalformedExpiredAndRefreshTokens() throws Exception {
        String path = "/api/activities/" + own;
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", "Bearer broken")).andExpect(status().isUnauthorized());
        var expired = new JwtUtil();
        org.springframework.test.util.ReflectionTestUtils.setField(expired, "secret", "local-test-signing-key-only-not-a-real-secret-12345678901234567890");
        org.springframework.test.util.ReflectionTestUtils.setField(expired, "expiration", -1000L);
        mvc.perform(get(path).header("Authorization", "Bearer " + expired.generateToken(users.findAll().get(0).getId())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", "Bearer " + jwt.generateRefreshToken(1L))).andExpect(status().isUnauthorized());
    }
    @Test void invalidParametersAndMissingRecordsReturnClientErrors() throws Exception {
        for (String path : List.of("/api/activities/0", "/api/wrong-answer/-1", "/api/ocr-results?studentId=0",
                "/api/activities/abc", "/api/ocr-results")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + parent)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/ocr-results/999999?studentId=" + own).header("Authorization", "Bearer " + parent))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/ocr-results?studentId=999999").header("Authorization", "Bearer " + parent))
                .andExpect(status().isForbidden());
    }
    @Test void onlyOwningAcademyCanWriteBoundedFeedbackAndParentCannotForgeIt() throws Exception {
        var r = evaluation("feedback"); r.setInstructorFeedback("forged");
        var content = mvc.perform(post("/api/activities").header("Authorization", "Bearer " + parent)
                .contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorFeedback").doesNotExist()).andReturn().getResponse().getContentAsString();
        var id = json.readTree(content).get("id").asLong();
        String path = "/api/activities/" + id + "/feedback";
        mvc.perform(patch(path).header("Authorization", "Bearer " + parent).contentType("application/json")
                .content("{\"instructorFeedback\":\"forged\"}")).andExpect(status().isForbidden());
        mvc.perform(patch(path).header("Authorization", "Bearer " + academy).contentType("application/json")
                .content(json.writeValueAsString(Map.of("instructorFeedback", "한".repeat(1001))))).andExpect(status().isBadRequest());
        mvc.perform(patch(path).header("Authorization", "Bearer " + academy).contentType("application/json")
                .content("{\"instructorFeedback\":\"정상\"}")).andExpect(status().isOk());
        assertThat(activities.saveActivity(r).getInstructorFeedback()).isEqualTo("정상");
    }
    private <T> List<T> concurrent(Callable<T> action) throws Exception {
        var pool = Executors.newFixedThreadPool(6);
        var start = new CountDownLatch(1);
        try {
            var futures = IntStream.range(0,6).mapToObj(i -> pool.submit(() -> { start.await(); return action.call(); })).toList();
            start.countDown(); var results = new ArrayList<T>();
            for (var future : futures) results.add(future.get(25, TimeUnit.SECONDS));
            return results;
        } finally { pool.shutdownNow(); }
    }
    @Test void simultaneousOcrRetriesSaveOnceAndCountWrongAnswerOnce() throws Exception {
        var r = scan("same", "국어", 1, 3);
        var results = concurrent(() -> ocr.save(r));
        assertThat(results).extracting(OcrResultResponse::id).containsOnly(results.get(0).id());
        assertThat(ocr.getByStudent(own,null)).hasSize(1);
        assertThat(wrong.getWrongAnswers(own,null)).singleElement().satisfies(w -> assertThat(w.getFailCount()).isEqualTo(1));
    }
    @Test void simultaneousDifferentScansDoNotLoseWrongCountsOrMixSubjects() throws Exception {
        concurrent(() -> ocr.save(scan(UUID.randomUUID().toString(), "국어", 1, 3)));
        assertThat(wrong.getWrongAnswers(own, "국어")).singleElement().satisfies(w -> assertThat(w.getFailCount()).isEqualTo(6));
        ocr.save(scan("math", "수학", 1, 3));
        assertThat(wrong.getWrongAnswers(own, "수학")).singleElement().satisfies(w -> assertThat(w.getFailCount()).isEqualTo(1));
        assertThat(wrong.getWrongAnswers(own, "국어")).singleElement().satisfies(w -> assertThat(w.getFailCount()).isEqualTo(6));
    }
    @Test void simultaneousManualRetriesSurviveLostResponseAndRejectConflictingKey() throws Exception {
        var r = evaluation("same");
        var results = concurrent(() -> activities.saveActivity(r));
        assertThat(results).extracting(LearningActivityResponse::getId).containsOnly(results.get(0).getId());
        assertThat(activities.saveActivity(r).getId()).isEqualTo(results.get(0).getId());
        assertThat(activities.getActivitiesByStudent(own)).hasSize(1);
        r.setConcentrationScore(2);
        mvc.perform(post("/api/activities").header("Authorization", "Bearer " + parent).contentType("application/json")
                .content(json.writeValueAsString(r))).andExpect(status().isConflict());
    }
    @Test void legacyPwaRetryAndUpgradeAttachKeyWithoutCreatingAnotherActivity() throws Exception {
        var legacy = evaluation(null);
        var results = concurrent(() -> activities.saveActivity(legacy));
        var id = results.get(0).getId();
        assertThat(results).extracting(LearningActivityResponse::getId).containsOnly(id);
        var keyed = evaluation("upgraded");
        assertThat(activities.saveActivity(keyed).getId()).isEqualTo(id);
        assertThat(activities.saveActivity(keyed).getSubmissionId()).isEqualTo("upgraded");
        assertThat(activities.saveActivity(legacy).getId()).isEqualTo(id);
        assertThat(activities.getActivitiesByStudent(own)).hasSize(1);
        // 시각이 없는 구형 클라이언트의 서로 다른 학습을 임의로 합치지 않는다.
        legacy.setStudyStartTime(null);
        assertThat(activities.saveActivity(legacy).getId()).isNotEqualTo(activities.saveActivity(legacy).getId());
    }
    @Test void concurrentConflictingOcrKeysReturnConflictAndValidationHasNoPartialWrites() throws Exception {
        var counter = new java.util.concurrent.atomic.AtomicInteger();
        var results = concurrent(() -> {
            try { ocr.save(scan("conflict", counter.getAndIncrement() % 2 == 0 ? "국어" : "수학", 1, 1)); return 200; }
            catch (org.springframework.web.server.ResponseStatusException e) { return e.getStatusCode().value(); }
        });
        assertThat(results).contains(200, 409).containsOnly(200, 409);
        assertThat(ocr.getByStudent(own, null)).hasSize(1);
        assertThat(wrong.getWrongAnswers(own, null)).singleElement().satisfies(w -> assertThat(w.getFailCount()).isEqualTo(1));
        var q = scan("bad", "영어", 1, 1).questions().get(0);
        var invalid = new OcrResultRequest(own, "영어", "bad", 2, 0, List.of(q, q));
        mvc.perform(post("/api/ocr-results").header("Authorization", "Bearer " + parent).contentType("application/json")
                .content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        assertThat(ocr.getByStudent(own, "영어")).isEmpty();
        assertThat(wrong.getWrongAnswers(own, "영어")).isEmpty();
    }
    @Test void acceptsMaximumOcrBatchAndUnicodeTextAndRejectsOverLimit() throws Exception {
        var r = scan("maximum", "국어", 1000, 10);
        mvc.perform(post("/api/ocr-results").header("Authorization", "Bearer " + parent).contentType("application/json")
                .content(json.writeValueAsString(r))).andExpect(status().isOk());
        assertThat(ocr.getByStudent(own, null).get(0).questions()).hasSize(1000);
        var longText = scan("long-text", "국어", 1, 10000);
        mvc.perform(post("/api/ocr-results").header("Authorization", "Bearer " + parent).contentType("application/json")
                .content(json.writeValueAsString(longText))).andExpect(status().isOk());
        for (var invalid : List.of(scan("too-many", "국어", 1001, 1), scan("too-long", "국어", 1, 10001))) {
            mvc.perform(post("/api/ocr-results").header("Authorization", "Bearer " + parent).contentType("application/json")
                    .content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        }
    }
    @Test void validatesCalendarTimeDurationScoreAndMeasurementBounds() throws Exception {
        var r = evaluation("bounds"); r.setStudyDurationMinutes(1440);
        mvc.perform(post("/api/activities").header("Authorization", "Bearer " + parent).contentType("application/json")
                .content(json.writeValueAsString(r))).andExpect(status().isOk());
        for (int duration : List.of(0, -1, 1441, Integer.MAX_VALUE)) {
            r.setStudyDurationMinutes(duration);
            mvc.perform(post("/api/activities").header("Authorization", "Bearer " + parent).contentType("application/json")
                    .content(json.writeValueAsString(r))).andExpect(status().isBadRequest());
        }
        var body = json.writeValueAsString(evaluation("invalid"));
        for (String invalid : List.of(body.replace("2026-11-05", "2026-02-30"), body.replace("10:17:00", "25:00:00"),
                body.replace("\"understandingScore\":5", "\"understandingScore\":6"))) {
            mvc.perform(post("/api/activities").header("Authorization", "Bearer " + parent).contentType("application/json")
                    .content(invalid)).andExpect(status().isBadRequest());
        }
    }
}
