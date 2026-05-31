package com.i_route.backend.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCharsetInitializer {

    private final JdbcTemplate jdbcTemplate;

    private static final List<String> AI_TABLES = List.of(
            "grade",
            "learning_activity",
            "ai_recommendation",
            "wrong_answer",
            "wrong_answer_entity",
            "student",
            "student_info",
            "study_material",
            "study_log",
            "target_goal",
            "review_notification",
            "grade_entity",
            "question"
    );

    @EventListener(ApplicationReadyEvent.class)
    public void convertCharset() {
        for (String table : AI_TABLES) {
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE `" + table + "` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                );
                log.info("charset 변환 완료: {}", table);
            } catch (Exception e) {
                log.debug("charset 변환 스킵 (테이블 없음 or 이미 utf8mb4): {}", table);
            }
        }
    }
}
