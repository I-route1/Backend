package com.i_route.backend.ai;

import com.i_route.backend.ai.entity.StudyMaterial;
import com.i_route.backend.ai.repository.GradeRepository;
import com.i_route.backend.ai.service.AiRecommendationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import static org.assertj.core.api.Assertions.*;

/** 과거 VARCHAR 열에 숫자 ID와 S-0001 형식 ID가 함께 남은 운영 데이터 형태를 재현한다. */
@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "logging.level.org.hibernate.SQL=OFF"})
@ContextConfiguration(classes = LegacyGradeRecommendationJpaTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LegacyGradeRecommendationJpaTest {
    @Configuration @EnableAutoConfiguration
    @EntityScan("com.i_route.backend")
    @EnableJpaRepositories("com.i_route.backend.ai.repository")
    @Import(AiRecommendationService.class)
    static class Config {}

    @Autowired EntityManager em;
    @Autowired GradeRepository grades;
    @Autowired AiRecommendationService recommendations;

    @Test void legacyStringIdsDoNotBreakAggregationOrBecomeCurrentStudentPeers() {
        // 이 테스트 전용 메모리 DB만 옛 열 형식으로 바꾼다.
        em.createNativeQuery("ALTER TABLE grade ALTER COLUMN student_id VARCHAR(64)").executeUpdate();
        em.createNativeQuery("INSERT INTO grade (student_id, score, grade_level, percentile) VALUES "
                + "('14', 80, 2, 70), ('15', 79, 2, 70), ('S-0001', 81, 2, 70), (NULL, 80, 2, 70)").executeUpdate();
        em.persist(StudyMaterial.builder().title("추천 자료").materialType("교재").level(4).subjectId(2L).build());
        em.flush(); em.clear();

        assertThat(grades.getAverageScoreByStudent(14L)).isEqualTo(80.0);
        assertThat(recommendations.recommendByPeerContent(14L)).singleElement()
                .satisfies(r -> assertThat(r.getMatchReason()).contains("1명"));
        assertThat(em.createNativeQuery("SELECT COUNT(*) FROM grade WHERE student_id = 'S-0001'").getSingleResult())
                .isEqualTo(1L);
    }
}
