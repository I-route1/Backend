package com.i_route.backend.ai;

import com.i_route.backend.ai.config.DataInitializer;
import com.i_route.backend.ai.repository.*;
import com.i_route.backend.board.config.BoardDataInitializer;
import com.i_route.backend.board.repository.BoardRepository;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.driver.repository.DriverRepository;
import com.i_route.backend.gps.domain.route.repository.RouteRepository;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.gps.global.init.GpsDummyDataInitializer;
import com.i_route.backend.user.repository.AcademyRepository;
import com.i_route.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DemoInitializerConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
            .withUserConfiguration(DataInitializer.class, GpsDummyDataInitializer.class, BoardDataInitializer.class)
            .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
            .withBean(StudyMaterialRepository.class, () -> mock(StudyMaterialRepository.class))
            .withBean(QuestionRepository.class, () -> mock(QuestionRepository.class))
            .withBean(StudentEntityRepository.class, () -> mock(StudentEntityRepository.class))
            .withBean(GradeRepository.class, () -> mock(GradeRepository.class))
            .withBean(GradeEntityRepository.class, () -> mock(GradeEntityRepository.class))
            .withBean(LearningActivityRepository.class, () -> mock(LearningActivityRepository.class))
            .withBean(StudyLogRepository.class, () -> mock(StudyLogRepository.class))
            .withBean(WrongAnswerEntityRepository.class, () -> mock(WrongAnswerEntityRepository.class))
            .withBean(WrongAnswerRepository.class, () -> mock(WrongAnswerRepository.class))
            .withBean(StudentRepository.class, () -> mock(StudentRepository.class))
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(AcademyRepository.class, () -> mock(AcademyRepository.class))
            .withBean(DriverRepository.class, () -> mock(DriverRepository.class))
            .withBean(RouteRepository.class, () -> mock(RouteRepository.class))
            .withBean(RouteStopRepository.class, () -> mock(RouteStopRepository.class))
            .withBean(BusRepository.class, () -> mock(BusRepository.class))
            .withBean(BoardRepository.class, () -> mock(BoardRepository.class));

    @Test
    void defaultKeepsExistingDemoInitialization() {
        contextRunner.run(context -> assertThat(context).hasNotFailed()
                .hasSingleBean(DataInitializer.class).hasSingleBean(GpsDummyDataInitializer.class)
                .hasSingleBean(BoardDataInitializer.class));
    }

    @Test
    void disabledPreventsBothDemoInitializersAndKeepsRequiredBoards() {
        contextRunner.withPropertyValues("app.demo-seed.enabled=false").run(context -> assertThat(context)
                .hasNotFailed().doesNotHaveBean(DataInitializer.class).doesNotHaveBean(GpsDummyDataInitializer.class)
                .hasSingleBean(BoardDataInitializer.class));
    }

    @Test
    void explicitlyEnabledKeepsLocalDemoInitialization() {
        contextRunner.withPropertyValues("app.demo-seed.enabled=true").run(context -> assertThat(context)
                .hasNotFailed().hasSingleBean(DataInitializer.class).hasSingleBean(GpsDummyDataInitializer.class));
    }

    @Test
    void enabledDoesNotActivateGpsInitializerOutsideLocalProfile() {
        contextRunner.withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues("app.demo-seed.enabled=true").run(context -> assertThat(context)
                        .hasNotFailed().hasSingleBean(DataInitializer.class).doesNotHaveBean(GpsDummyDataInitializer.class));
    }
}
