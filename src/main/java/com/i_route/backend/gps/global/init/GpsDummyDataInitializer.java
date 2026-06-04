package com.i_route.backend.gps.global.init;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.bus.entity.OperationStatus;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.driver.entity.Driver;
import com.i_route.backend.gps.domain.driver.repository.DriverRepository;
import com.i_route.backend.gps.domain.route.entity.Route;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.repository.RouteRepository;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.user.repository.AcademyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;

@Component
@Profile("local")
@RequiredArgsConstructor
public class GpsDummyDataInitializer implements CommandLineRunner {

    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final BusRepository busRepository;
    private final StudentRepository studentRepository;
    private final AcademyRepository academyRepository;

    private static final String[] FIRST_NAMES = {"민준", "서연", "준혁", "지은", "현우", "정현", "미영", "수현", "태희", "혜진",
            "현정", "은정", "민지", "유진", "은지", "수진", "지수", "경은", "하나", "슬기"};
    private static final String[] LAST_NAMES = {"김", "이", "박", "최", "정", "조", "윤", "장", "임", "전", "한", "홍", "신", "곽", "성", "원", "문", "구", "노", "도"};
    private static final Student.LearningTendency[] TENDENCIES = {Student.LearningTendency.VISUAL, Student.LearningTendency.AUDITORY, Student.LearningTendency.KINESTHETIC};
    private static final String[] NOTES = {"집중력 향상 필요", "참여도 높음", "개념 이해 부족", "꾸준한 노력형", "창의적 사고력 우수", "적극적 학습태도", "복습 강화 필요", "팀 활동 적극적"};

    @Override
    public void run(String... args) {
        if (studentRepository.count() > 0) {
            return;
        }

        List<Academy> academies = academyRepository.findAll();
        if (academies.isEmpty()) {
            return;
        }

        // 첫 번째 학원과 기사(userId 19) 연결
        Academy firstAcademy = academies.get(0);
        Driver driver = driverRepository.findById(19L).orElse(null);
        if (driver == null) {
            driver = driverRepository.save(
                    Driver.builder()
                            .id(19L)
                            .userId(19L)
                            .name("이기사")
                            .phoneNumber("010-2222-3333")
                            .build()
            );
        }
        firstAcademy.setDriver(driver);
        academyRepository.save(firstAcademy);

        // 5개 학원, 학원당 50명씩 250명 생성
        // 테스트 학부모(userId 14)에 첫 5개 학생만 할당
        Random random = new Random(42);
        int studentId = 1;
        final int PARENT_USER_ID = 14; // 테스트 학부모 계정

        for (Academy academy : academies) {
            List<Student> studentsForAcademy = new ArrayList<>();

            // 성적대 분포: 최상위(1등급) 5명, 상위(2등급) 10명, 중위(3등급) 20명, 하위(4-5등급) 15명
            int[] gradeDistribution = {5, 10, 20, 10, 5};

            for (int gradeLevel = 1; gradeLevel <= 5; gradeLevel++) {
                for (int count = 0; count < gradeDistribution[gradeLevel - 1]; count++) {
                    String name = LAST_NAMES[random.nextInt(LAST_NAMES.length)] +
                                 FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];

                    double koreanGrade = getGradeRange(gradeLevel, random);

                    // studentId 1~5만 테스트 학부모에 할당, 나머지는 null
                    Long parentId = (studentId >= 1 && studentId <= 5) ? (long) PARENT_USER_ID : null;

                    Student student = Student.builder()
                            .name(name)
                            .academyId(academy.getAcademyId())
                            .parentId(parentId)
                            .expectedDropOffTime(LocalTime.of(22 + (studentId % 2), studentId % 60))
                            .currentLevel(gradeLevel)
                            .learningTendency(TENDENCIES[random.nextInt(TENDENCIES.length)])
                            .currentKoreanGrade(koreanGrade)
                            .studyTime((double) (2 + random.nextInt(4)))
                            .studentNote(NOTES[random.nextInt(NOTES.length)])
                            .recommendContext(getRecommendContext(gradeLevel))
                            .build();

                    studentsForAcademy.add(student);
                    studentId++;
                }
            }

            studentRepository.saveAll(studentsForAcademy);
        }
    }

    private double getGradeRange(int gradeLevel, Random random) {
        return switch (gradeLevel) {
            case 1 -> 90 + random.nextDouble() * 10;      // 90~100
            case 2 -> 80 + random.nextDouble() * 10;      // 80~90
            case 3 -> 70 + random.nextDouble() * 10;      // 70~80
            case 4 -> 60 + random.nextDouble() * 10;      // 60~70
            case 5 -> 50 + random.nextDouble() * 10;      // 50~60
            default -> 70 + random.nextDouble() * 10;
        };
    }

    private String getRecommendContext(int gradeLevel) {
        return switch (gradeLevel) {
            case 1 -> "심화 과정 추천";
            case 2 -> "상위권 진입 목표";
            case 3 -> "기초 강화 필요";
            case 4 -> "개념 이해 집중";
            case 5 -> "전면 재학습 권장";
            default -> "학습 계획 수립 필요";
        };
    }
}
