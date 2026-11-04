package com.i_route.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.ai.controller.*;
import com.i_route.backend.ai.dto.*;
import com.i_route.backend.ai.service.*;
import com.i_route.backend.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LearningRecordsApiTest {
    final OcrResultService ocr = mock(OcrResultService.class);
    final WrongAnswerService wrong = mock(WrongAnswerService.class);
    final LearningActivityService activities = mock(LearningActivityService.class);
    MockMvc mvc;
    final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new OcrResultController(ocr),
                new WrongAnswerController(wrong), new LearningActivityController(activities))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private OcrResultRequest input(int total, int correct, List<OcrResultRequest.Question> questions) {
        return new OcrResultRequest(14L, "국어", "scan-1", total, correct, questions);
    }
    private OcrResultRequest.Question question(String text) {
        return new OcrResultRequest.Question("Q1", "돼지", text, 0.5, false, "받침", null);
    }
    private void postOcr(OcrResultRequest request, int status) throws Exception {
        mvc.perform(post("/api/ocr-results").contentType("application/json")
                .content(json.writeValueAsString(request))).andExpect(status().is(status));
    }

    @Test
    void acceptsBlankRecognitionButRejectsMismatchedTotalsMissingTextAndDuplicateQuestions() throws Exception {
        postOcr(input(1, 0, List.of(question(""))), 200);
        postOcr(input(2, 0, List.of(question("되지"))), 400);
        postOcr(input(1, 1, List.of(question("되지"))), 400);
        postOcr(input(1, 0, List.of(question(null))), 400);
        postOcr(input(2, 0, List.of(question(""), question(""))), 400);
        verify(ocr, times(1)).save(any());
    }

    @Test
    void supportsAllSubjectsAndOptionalSubjectFilter() throws Exception {
        when(wrong.getWrongAnswers(eq(14L), any())).thenReturn(List.of());
        mvc.perform(get("/api/wrong-answer/14")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/wrong-answer/14").param("subject", "국어")).andExpect(status().isOk());
        verify(wrong).getWrongAnswers(14L, null);
        verify(wrong).getWrongAnswers(14L, "국어");
        mvc.perform(get("/api/ocr-results").param("studentId", "14")).andExpect(status().isOk());
        mvc.perform(get("/api/ocr-results/1").param("studentId", "14")).andExpect(status().isOk());
        verify(ocr).getByStudent(14L, null);
        verify(ocr).get(1L, 14L);
    }

    @Test
    void acceptsLegacyManualAndCameraButRejectsInvalidMeasurement() throws Exception {
        String base = """
                {"studentId":14,"subject":"수학","studyDate":"2026-11-04",
                 "studyDurationMinutes":30,"understandingScore":4,"concentrationScore":3%s}
                """;
        for (String extra : List.of("", ",\"measurementSource\":\"CAMERA\",\"focusedRatio\":0.6,\"drowsyRatio\":0.2")) {
            mvc.perform(post("/api/activities").contentType("application/json").content(base.formatted(extra)))
                    .andExpect(status().isOk());
        }
        for (String extra : List.of(
                ",\"measurementSource\":\"CAMERA\"",
                ",\"focusedRatio\":1.1",
                ",\"drowsyRatio\":-0.1",
                ",\"focusedRatio\":0.8,\"drowsyRatio\":0.3",
                ",\"measurementSource\":\"UNKNOWN\"")) {
            mvc.perform(post("/api/activities").contentType("application/json").content(base.formatted(extra)))
                    .andExpect(status().isBadRequest());
        }
        verify(activities, times(2)).saveActivity(any());
    }
}
