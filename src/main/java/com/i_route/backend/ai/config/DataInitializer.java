package com.i_route.backend.ai.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.ai.entity.*;
import com.i_route.backend.ai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final StudyMaterialRepository studyMaterialRepository;
    private final QuestionRepository questionRepository;
    private final StudentEntityRepository studentEntityRepository;
    private final GradeRepository gradeRepository;
    private final GradeEntityRepository gradeEntityRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final StudyLogRepository studyLogRepository;
    private final WrongAnswerEntityRepository wrongAnswerEntityRepository;
    private final WrongAnswerRepository wrongAnswerRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedStudyMaterials();
        seedQuestions();
        seedStudents();
        seedWrongAnswers();
        seedStringWrongAnswers();
        seedGrades();
        seedGradeEntities();
        seedLearningActivities();
        seedStudyLogs();
    }

    // ─── 기존: 학습 자료 ──────────────────────────────────────
    private void seedStudyMaterials() {
        if (studyMaterialRepository.count() > 0) return;

        List<StudyMaterial> materials = new ArrayList<>();
        Object[][] data = {
            {"국어 기초 독해력 워크북", "교재", 1, 1L}, {"국어 기초 문법 완성", "교재", 2, 1L},
            {"중학 국어 문학 독해", "교재", 3, 1L}, {"고등 국어 비문학 실전", "교재", 4, 1L},
            {"수능 국어 심화 모의고사", "교재", 5, 1L}, {"EBS 국어 기초 강좌", "인강", 1, 1L},
            {"국어 개념 완성 강좌", "인강", 2, 1L}, {"중학 국어 내신 완성", "인강", 3, 1L},
            {"고등 국어 1등급 전략", "인강", 4, 1L}, {"수능 국어 파이널 강좌", "인강", 5, 1L},
            {"수학 기초 연산 워크북", "교재", 1, 2L}, {"중학 수학 개념 완성", "교재", 2, 2L},
            {"중학 수학 실력 업그레이드", "교재", 3, 2L}, {"고등 수학 유형별 공략", "교재", 4, 2L},
            {"수능 수학 심화 문제집", "교재", 5, 2L}, {"수학 기초 개념 강좌", "인강", 1, 2L},
            {"중학 수학 핵심 정리", "인강", 2, 2L}, {"고등 수학 개념+유형", "인강", 3, 2L},
            {"수능 수학 1등급 공략", "인강", 4, 2L}, {"수학 킬러문항 집중 공략", "인강", 5, 2L},
            {"영어 기초 단어 암기장", "교재", 1, 3L}, {"중학 영어 어법 완성", "교재", 2, 3L},
            {"중학 영어 독해 실전", "교재", 3, 3L}, {"고등 영어 구문 독해", "교재", 4, 3L},
            {"수능 영어 심화 독해", "교재", 5, 3L}, {"영어 기초 회화 강좌", "인강", 1, 3L},
            {"중학 영어 내신 대비", "인강", 2, 3L}, {"고등 영어 독해 전략", "인강", 3, 3L},
            {"수능 영어 1등급 비법", "인강", 4, 3L}, {"수능 영어 파이널 모의고사", "인강", 5, 3L},
            {"과학 기초 탐구 워크북", "교재", 1, 4L}, {"중학 과학 개념 완성", "교재", 2, 4L},
            {"중학 과학 실전 문제집", "교재", 3, 4L}, {"고등 통합과학 핵심 정리", "교재", 4, 4L},
            {"수능 과학탐구 심화 문제집", "교재", 5, 4L}, {"과학 기초 개념 강좌", "인강", 1, 4L},
            {"중학 과학 핵심 강의", "인강", 2, 4L}, {"고등 물리/화학 개념 강좌", "인강", 3, 4L},
            {"수능 과탐 1등급 전략", "인강", 4, 4L}, {"과학탐구 킬러문항 공략", "인강", 5, 4L},
            {"사회 기초 개념 워크북", "교재", 1, 5L}, {"중학 사회 핵심 정리", "교재", 2, 5L},
            {"중학 역사 실전 문제집", "교재", 3, 5L}, {"고등 사회문화 개념 완성", "교재", 4, 5L},
            {"수능 사탐 심화 문제집", "교재", 5, 5L}, {"사회 기초 개념 강좌", "인강", 1, 5L},
            {"중학 사회/역사 핵심 강의", "인강", 2, 5L}, {"고등 사회문화 1등급 전략", "인강", 3, 5L},
            {"수능 사탐 선택과목 집중", "인강", 4, 5L}, {"사탐 킬러문항 실전 대비", "인강", 5, 5L},
        };
        for (Object[] row : data) {
            materials.add(StudyMaterial.builder()
                    .title((String) row[0]).materialType((String) row[1])
                    .level((Integer) row[2]).subjectId((Long) row[3]).build());
        }
        studyMaterialRepository.saveAll(materials);
        log.info("[DataInitializer] StudyMaterial {}개 시드 완료", materials.size());
    }

    // ─── 기존: 문제 ───────────────────────────────────────────
    private void seedQuestions() {
        if (questionRepository.count() > 0) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("questions_seed.json");
            InputStream is = resource.getInputStream();
            List<Map<String, Object>> raw = mapper.readValue(is, new TypeReference<>() {});
            List<Question> questions = raw.stream()
                    .map(m -> Question.builder()
                            .subjectId(((Number) m.get("subjectId")).longValue())
                            .conceptTag((String) m.get("conceptTag"))
                            .content((String) m.get("content"))
                            .difficulty((Integer) m.get("difficulty"))
                            .build())
                    .toList();
            questionRepository.saveAll(questions);
            log.info("[DataInitializer] Question {}개 시드 완료", questions.size());
        } catch (Exception e) {
            log.warn("[DataInitializer] questions_seed.json 로드 실패, 스킵: {}", e.getMessage());
        }
    }

    // ─── 신규: 학생 엔티티 (Long ID) ─────────────────────────
    private void seedStudents() {
        if (studentEntityRepository.count() >= 5) return;

        // name, tendency, level
        Object[][] data = {
            {"홍민준", StudyTendency.VISUAL,      4},  // ID 1: 상위권 시각형
            {"김서연", StudyTendency.AUDITORY,    3},  // ID 2: 중위권 청각형
            {"이준혁", StudyTendency.KINESTHETIC, 2},  // ID 3: 하위권 행동형
            {"박지은", StudyTendency.VISUAL,      5},  // ID 4: 최상위 시각형
            {"최현우", StudyTendency.AUDITORY,    3},  // ID 5: 중위권 청각형
        };
        List<StudentEntity> students = new ArrayList<>();
        for (Object[] row : data) {
            students.add(StudentEntity.builder()
                    .name((String) row[0])
                    .learningTendency((StudyTendency) row[1])
                    .currentLevel((Integer) row[2])
                    .build());
        }
        studentEntityRepository.saveAll(students);
        log.info("[DataInitializer] StudentEntity {}개 시드 완료", students.size());
    }

    // ─── 신규: WrongAnswerEntity (복습 시험지 생성용) ─────────
    private void seedWrongAnswers() {
        if (wrongAnswerEntityRepository.count() > 0) return;

        List<Question> questions = questionRepository.findAll();
        if (questions.isEmpty()) {
            log.warn("[DataInitializer] Question 없음 — WrongAnswer 시딩 스킵");
            return;
        }

        // 최대 8문제 사용 (없으면 있는 만큼)
        int limit = Math.min(8, questions.size());
        List<WrongAnswerEntity> answers = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            Long qId = questions.get(i).getQuestionId();
            // 학생1(홍민준): 최근에 틀린 문제 3개
            if (i < 3) answers.add(WrongAnswerEntity.builder()
                    .studentId(1L).questionId(qId).failCount(i + 1).build());
            // 학생3(이준혁): 더 많이 틀린 문제 5개
            answers.add(WrongAnswerEntity.builder()
                    .studentId(3L).questionId(qId).failCount(i + 2).build());
            // 학생4(박지은): 어려운 문제 1~2개
            if (i >= limit - 2) answers.add(WrongAnswerEntity.builder()
                    .studentId(4L).questionId(qId).failCount(1).build());
        }

        wrongAnswerEntityRepository.saveAll(answers);
        log.info("[DataInitializer] WrongAnswerEntity {}개 시드 완료", answers.size());
    }

    // ─── 신규: WrongAnswer (Long studentId) ──
    private void seedStringWrongAnswers() {
        // 학생 1 데이터가 없으면 전체 재시딩
        boolean s1Exists = !wrongAnswerRepository
                .findTopWeaknessByStudentIdAndSubject(1L, "수학").isEmpty();
        if (s1Exists) return;

        // studentId(Long), subject, questionId, conceptTag, errorType
        Object[][] data = {
            // 학생 1 (홍민준): 수학 취약
            {1L,"수학","Q-001","삼각함수 덧셈공식",        "CONCEPT_GAP"},
            {1L,"수학","Q-002","삼각함수 응용 문제",       "CARELESS_MISTAKE"},
            {1L,"영어","Q-003","가정법 과거완료 형태",      "CONCEPT_GAP"},
            // 학생 3 (이준혁): 수학·영어 취약
            {3L,"수학","Q-004","수열의 합 공식",            "CONCEPT_GAP"},
            {3L,"수학","Q-005","등차수열 일반항",           "MEMORIZATION_GAP"},
            {3L,"수학","Q-006","이차방정식 판별식",         "CONCEPT_GAP"},
            {3L,"영어","Q-007","도치구문 강조 용법",        "CONCEPT_GAP"},
            {3L,"영어","Q-008","수동태 완료형",             "MEMORIZATION_GAP"},
            // 학생 4 (박지은): 심화 수학 취약
            {4L,"수학","Q-009","미분 chain rule 응용",      "CARELESS_MISTAKE"},
        };

        List<WrongAnswer> answers = new ArrayList<>();
        for (Object[] r : data) {
            ErrorType et = null;
            try { et = ErrorType.valueOf((String) r[4]); } catch (Exception ignored) {}
            answers.add(WrongAnswer.builder()
                    .studentId((Long) r[0])
                    .subject((String) r[1])
                    .questionId((String) r[2])
                    .conceptTag((String) r[3])
                    .errorType(et)
                    .build());
        }
        wrongAnswerRepository.saveAll(answers);
        log.info("[DataInitializer] WrongAnswer(Long) {}개 시드 완료", answers.size());
    }

    // ─── 신규: Grade (Long studentId, weakConceptTag 포함) ─
    private void seedGrades() {
        // weakConceptTag가 포함된 학생 1 데이터가 있으면 이미 시딩됐으므로 스킵
        boolean alreadySeeded = gradeRepository.findByStudentIdOrderByExamDateAsc(1L)
                .stream().anyMatch(g -> g.getWeakConceptTag() != null && !g.getWeakConceptTag().isEmpty());
        if (alreadySeeded) return;

        // 기존 학생 1~4 데이터 클리어 후 재시딩
        for (Long sid : List.of(1L, 2L, 3L, 4L)) {
            gradeRepository.deleteAll(gradeRepository.findByStudentIdOrderByExamDateAsc(sid));
        }

        // studentId(Long), subject, score, gradeLevel, percentile, examType, examDate, weakConceptTag
        // 1=홍민준(상승), 2=김서연(안정), 3=이준혁(연속하락→위험), 4=박지은(최상위)
        Object[][] data = {
            // ── 홍민준 (1): 수학 상승 추세 ──
            {1L,"수학", 72, 4, 58.0, "3월모의고사",  LocalDate.of(2025,3,15),  "이차방정식의 근과 계수"},
            {1L,"수학", 78, 3, 65.0, "중간고사",    LocalDate.of(2025,5,10),  "함수의 최댓값 최솟값"},
            {1L,"수학", 84, 2, 74.0, "6월모의고사",  LocalDate.of(2025,6,12),  "등차수열 일반항"},
            {1L,"수학", 88, 2, 80.0, "기말고사",    LocalDate.of(2025,7,8),   "이차함수 꼭짓점"},
            {1L,"수학", 92, 1, 88.0, "9월모의고사",  LocalDate.of(2025,9,11),  "삼각함수 기본공식"},
            {1L,"영어", 85, 2, 76.0, "중간고사",    LocalDate.of(2025,5,10),  "관계대명사 that/which"},
            {1L,"영어", 88, 2, 80.0, "기말고사",    LocalDate.of(2025,7,8),   "분사구문 의미상 주어"},
            {1L,"영어", 90, 1, 85.0, "9월모의고사",  LocalDate.of(2025,9,11),  "가정법 과거완료"},
            {1L,"국어", 80, 3, 70.0, "중간고사",    LocalDate.of(2025,5,10),  "비문학 추론적 이해"},
            {1L,"국어", 83, 2, 74.0, "기말고사",    LocalDate.of(2025,7,8),   "고전소설 인물 심리"},
            {1L,"국어", 86, 2, 78.0, "9월모의고사",  LocalDate.of(2025,9,11),  "현대시 화자 태도"},

            // ── 김서연 (2): 성적 안정 유지 ──
            {2L,"수학", 65, 4, 50.0, "중간고사",    LocalDate.of(2025,5,10),  "분수 나눗셈 역수"},
            {2L,"수학", 67, 4, 52.0, "기말고사",    LocalDate.of(2025,7,8),   "비와 비율 계산"},
            {2L,"수학", 66, 4, 51.0, "9월모의고사",  LocalDate.of(2025,9,11),  "방정식의 활용"},
            {2L,"영어", 70, 3, 55.0, "중간고사",    LocalDate.of(2025,5,10),  "현재완료 용법"},
            {2L,"영어", 72, 3, 58.0, "기말고사",    LocalDate.of(2025,7,8),   "부정사 명사적 용법"},
            {2L,"국어", 68, 4, 53.0, "중간고사",    LocalDate.of(2025,5,10),  "문학 표현기법 반어"},
            {2L,"국어", 70, 3, 55.0, "기말고사",    LocalDate.of(2025,7,8),   "비문학 핵심어 파악"},

            // ── 이준혁 (3): 수학·영어 연속 하락 → 위험 ──
            {3L,"수학", 80, 3, 68.0, "3월모의고사",  LocalDate.of(2025,3,15),  "일차방정식 풀이"},
            {3L,"수학", 72, 3, 58.0, "중간고사",    LocalDate.of(2025,5,10),  "이차방정식 판별식"},
            {3L,"수학", 63, 4, 48.0, "6월모의고사",  LocalDate.of(2025,6,12),  "함수 합성"},
            {3L,"수학", 55, 5, 38.0, "기말고사",    LocalDate.of(2025,7,8),   "수열의 합 공식"},
            {3L,"영어", 75, 3, 60.0, "3월모의고사",  LocalDate.of(2025,3,15),  "관계부사 where/when"},
            {3L,"영어", 68, 4, 52.0, "중간고사",    LocalDate.of(2025,5,10),  "수동태 완료형"},
            {3L,"영어", 60, 4, 44.0, "기말고사",    LocalDate.of(2025,7,8),   "도치구문 강조"},
            {3L,"국어", 72, 3, 57.0, "중간고사",    LocalDate.of(2025,5,10),  "문학 갈등 구조"},
            {3L,"국어", 70, 3, 55.0, "기말고사",    LocalDate.of(2025,7,8),   "고전시가 어휘"},

            // ── 박지은 (4): 최상위권 꾸준 성장 ──
            {4L,"수학", 92, 1, 90.0, "중간고사",    LocalDate.of(2025,5,10),  "수열의 귀납적 정의"},
            {4L,"수학", 95, 1, 93.0, "기말고사",    LocalDate.of(2025,7,8),   "삼각함수 덧셈공식"},
            {4L,"수학", 98, 1, 97.0, "9월모의고사",  LocalDate.of(2025,9,11),  "지수로그 함수 미분"},
            {4L,"영어", 94, 1, 91.0, "중간고사",    LocalDate.of(2025,5,10),  "가정법 미래"},
            {4L,"영어", 96, 1, 95.0, "기말고사",    LocalDate.of(2025,7,8),   "복합관계대명사"},
            {4L,"국어", 90, 1, 87.0, "중간고사",    LocalDate.of(2025,5,10),  "현대소설 서술시점"},
            {4L,"국어", 93, 1, 91.0, "기말고사",    LocalDate.of(2025,7,8),   "시조 형식 평시조"},
        };

        List<Grade> grades = new ArrayList<>();
        for (Object[] r : data) {
            grades.add(Grade.builder()
                    .studentId((Long) r[0])
                    .subject((String) r[1])
                    .score((Integer) r[2])
                    .gradeLevel((Integer) r[3])
                    .percentile((Double) r[4])
                    .examType((String) r[5])
                    .examDate((LocalDate) r[6])
                    .weakConceptTag((String) r[7])
                    .build());
        }
        gradeRepository.saveAll(grades);
        log.info("[DataInitializer] Grade {}개 시드 완료 (weakConceptTag 포함)", grades.size());
    }

    // ─── 신규: GradeEntity (Long studentId, PredictionService용) ─
    private void seedGradeEntities() {
        // 학생 1/수학 + 학생 3/국어 모두 3건 이상 있어야 완전 시딩으로 간주
        boolean student1Done = gradeEntityRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(1L, 2L).size() >= 3;
        boolean student3Done = gradeEntityRepository.findByStudentIdAndSubjectIdOrderByTestDateAsc(3L, 1L).size() >= 3;
        if (student1Done && student3Done) return;

        // studentId(Long), subjectId(1=국어 2=수학 3=영어), score, testDate, percentile
        Object[][] data = {
            // 학생 1(홍민준): 수학 꾸준 상승
            {1L, 2L, 72, LocalDate.of(2025,3,15),  58.0},
            {1L, 2L, 78, LocalDate.of(2025,5,10),  65.0},
            {1L, 2L, 84, LocalDate.of(2025,6,12),  74.0},
            {1L, 2L, 88, LocalDate.of(2025,7,8),   80.0},
            {1L, 2L, 92, LocalDate.of(2025,9,11),  88.0},
            // 학생 1: 영어 상승
            {1L, 3L, 85, LocalDate.of(2025,5,10),  76.0},
            {1L, 3L, 88, LocalDate.of(2025,7,8),   80.0},
            {1L, 3L, 90, LocalDate.of(2025,9,11),  85.0},
            // 학생 1: 국어 완만 상승
            {1L, 1L, 80, LocalDate.of(2025,5,10),  70.0},
            {1L, 1L, 83, LocalDate.of(2025,7,8),   74.0},
            {1L, 1L, 86, LocalDate.of(2025,9,11),  78.0},

            // 학생 2(김서연): 수학 약간 변동
            {2L, 2L, 65, LocalDate.of(2025,5,10),  50.0},
            {2L, 2L, 67, LocalDate.of(2025,7,8),   52.0},
            {2L, 2L, 66, LocalDate.of(2025,9,11),  51.0},
            // 학생 2: 영어 소폭 상승
            {2L, 3L, 70, LocalDate.of(2025,5,10),  55.0},
            {2L, 3L, 72, LocalDate.of(2025,7,8),   58.0},
            {2L, 3L, 74, LocalDate.of(2025,9,11),  61.0},

            // 학생 3(이준혁): 수학 급락
            {3L, 2L, 80, LocalDate.of(2025,3,15),  68.0},
            {3L, 2L, 72, LocalDate.of(2025,5,10),  58.0},
            {3L, 2L, 63, LocalDate.of(2025,6,12),  48.0},
            {3L, 2L, 55, LocalDate.of(2025,7,8),   38.0},

            // 학생 4(박지은): 최상위 상승
            {4L, 2L, 92, LocalDate.of(2025,5,10),  90.0},
            {4L, 2L, 95, LocalDate.of(2025,7,8),   93.0},
            {4L, 2L, 98, LocalDate.of(2025,9,11),  97.0},
            {4L, 3L, 94, LocalDate.of(2025,5,10),  91.0},
            {4L, 3L, 96, LocalDate.of(2025,7,8),   95.0},

            // 학생 3(이준혁): 국어·영어 추가 (하락 추세)
            {3L, 1L, 75, LocalDate.of(2025,3,15),  60.0},
            {3L, 1L, 70, LocalDate.of(2025,5,10),  55.0},
            {3L, 1L, 65, LocalDate.of(2025,7,8),   49.0},
            {3L, 3L, 73, LocalDate.of(2025,3,15),  58.0},
            {3L, 3L, 67, LocalDate.of(2025,5,10),  51.0},
            {3L, 3L, 60, LocalDate.of(2025,7,8),   44.0},

            // 학생 4(박지은): 국어 추가 (상승 추세)
            {4L, 1L, 88, LocalDate.of(2025,5,10),  84.0},
            {4L, 1L, 91, LocalDate.of(2025,7,8),   88.0},
            {4L, 1L, 94, LocalDate.of(2025,9,11),  92.0},

            // 학생 5(최현우): 중위권 유지
            {5L, 2L, 68, LocalDate.of(2025,5,10),  53.0},
            {5L, 2L, 71, LocalDate.of(2025,7,8),   56.0},
            {5L, 2L, 70, LocalDate.of(2025,9,11),  55.0},
            {5L, 1L, 65, LocalDate.of(2025,5,10),  50.0},
            {5L, 1L, 68, LocalDate.of(2025,7,8),   53.0},
        };

        List<GradeEntity> entities = new ArrayList<>();
        for (Object[] r : data) {
            entities.add(GradeEntity.builder()
                    .studentId((Long) r[0])
                    .subjectId((Long) r[1])
                    .score((Integer) r[2])
                    .testDate((LocalDate) r[3])
                    .percentile((Double) r[4])
                    .build());
        }
        gradeEntityRepository.saveAll(entities);
        log.info("[DataInitializer] GradeEntity {}개 시드 완료", entities.size());
    }

    // ─── 신규: LearningActivity (골든타임 분석용) ─────────────
    private void seedLearningActivities() {
        // 학생 1/수학에 understandingScore >= 4 데이터가 있으면 이미 시딩됨
        boolean alreadySeeded = learningActivityRepository.findByStudentIdAndSubject(1L, "수학")
                .stream().anyMatch(a -> a.getUnderstandingScore() >= 4);
        if (alreadySeeded) return;

        // 기존 학생 1~5 데이터 클리어 후 재시딩
        for (Long sid : List.of(1L, 2L, 3L, 4L, 5L)) {
            learningActivityRepository.deleteAll(learningActivityRepository.findByStudentId(sid));
        }

        // studentId(Long), subject, studyDate, startHour, durationMin, understanding, concentration, feedback
        Object[][] data = {
            // 홍민준(1): 18시 전후 집중, 수학 주력
            {1L,"수학", LocalDate.of(2025,9,15), 18, 90, 4, 5, "분수 단원 집중도 매우 좋았음. 계산 실수가 줄어드는 추세."},
            {1L,"수학", LocalDate.of(2025,9,16), 18, 80, 4, 4, null},
            {1L,"수학", LocalDate.of(2025,9,17), 17, 70, 3, 4, null},
            {1L,"영어", LocalDate.of(2025,9,15), 20, 60, 3, 3, "발표력이 향상됨. 독해 속도는 아직 개선 필요."},
            {1L,"영어", LocalDate.of(2025,9,16), 19, 50, 3, 4, null},
            {1L,"국어", LocalDate.of(2025,9,18), 18, 45, 4, 3, null},
            {1L,"수학", LocalDate.of(2025,9,19), 18, 100, 5, 5, "삼각함수 개념 완벽 이해. 응용 문제도 혼자 풀어냄."},
            {1L,"수학", LocalDate.of(2025,9,20), 17, 90, 4, 5, null},

            // 김서연(2): 14시 전후 집중, 영어 주력
            {2L,"영어", LocalDate.of(2025,9,15), 14, 60, 3, 3, "현재완료 용법 반복 연습 필요."},
            {2L,"영어", LocalDate.of(2025,9,16), 15, 50, 3, 3, null},
            {2L,"수학", LocalDate.of(2025,9,17), 14, 40, 2, 3, "분수 계산 아직 불안정. 기초 개념 재확인 권장."},
            {2L,"수학", LocalDate.of(2025,9,18), 14, 45, 2, 2, null},
            {2L,"국어", LocalDate.of(2025,9,19), 16, 30, 3, 3, null},

            // 이준혁(3): 21시 야간, 짧은 집중 분산
            {3L,"수학", LocalDate.of(2025,9,15), 21, 30, 2, 2, "수열 공식 암기 부족. 기초부터 다시 잡을 필요 있음."},
            {3L,"수학", LocalDate.of(2025,9,16), 21, 25, 1, 2, null},
            {3L,"영어", LocalDate.of(2025,9,17), 22, 20, 2, 1, "도치구문 이해 저조. 예문 반복 학습 필요."},
            {3L,"국어", LocalDate.of(2025,9,18), 21, 20, 2, 2, null},

            // 박지은(4): 17시 집중, 전 과목 고른 학습
            {4L,"수학", LocalDate.of(2025,9,15), 17, 120, 5, 5, "미분 심화 완벽 소화. 다음 단계 진도 가능."},
            {4L,"수학", LocalDate.of(2025,9,16), 17, 110, 5, 5, null},
            {4L,"영어", LocalDate.of(2025,9,15), 19, 90, 5, 4, "복합관계대명사 완벽 이해."},
            {4L,"국어", LocalDate.of(2025,9,17), 17, 80, 4, 5, null},
            {4L,"수학", LocalDate.of(2025,9,18), 17, 120, 5, 5, null},

            // 최현우(5): 18시 주력, 수학·사회
            {5L,"수학", LocalDate.of(2025,9,15), 18, 60, 3, 4, null},
            {5L,"수학", LocalDate.of(2025,9,16), 18, 55, 3, 3, "방정식 활용 문제 반복 풀이 필요."},
            {5L,"사회", LocalDate.of(2025,9,17), 19, 40, 3, 3, null},
            {5L,"국어", LocalDate.of(2025,9,18), 17, 35, 3, 3, null},
        };

        List<LearningActivity> activities = new ArrayList<>();
        for (Object[] r : data) {
            activities.add(LearningActivity.builder()
                    .studentId((Long) r[0])
                    .subject((String) r[1])
                    .studyDate((LocalDate) r[2])
                    .studyStartTime(LocalTime.of((Integer) r[3], 0))
                    .studyDurationMinutes((Integer) r[4])
                    .understandingScore((Integer) r[5])
                    .concentrationScore((Integer) r[6])
                    .instructorFeedback((String) r[7])
                    .build());
        }
        learningActivityRepository.saveAll(activities);
        log.info("[DataInitializer] LearningActivity {}개 시드 완료", activities.size());
    }

    // ─── 신규: StudyLog (PredictionService 학습량 보너스용) ───
    private void seedStudyLogs() {
        // 학생 1의 수학(subjectId=2) StudyLog가 3건 미만이면 시딩
        if (studyLogRepository.findByStudentId(1L).size() >= 3) return;

        // studentId(Long), subjectId, durationMin, startTime
        Object[][] data = {
            {1L, 2L, 90,  LocalDateTime.of(2025,9,15,18,0)},
            {1L, 2L, 80,  LocalDateTime.of(2025,9,16,18,0)},
            {1L, 3L, 60,  LocalDateTime.of(2025,9,15,20,0)},
            {1L, 1L, 45,  LocalDateTime.of(2025,9,18,18,0)},
            {1L, 2L, 100, LocalDateTime.of(2025,9,19,18,0)},
            {2L, 3L, 60,  LocalDateTime.of(2025,9,15,14,0)},
            {2L, 2L, 40,  LocalDateTime.of(2025,9,17,14,0)},
            {3L, 2L, 30,  LocalDateTime.of(2025,9,15,21,0)},
            {3L, 3L, 20,  LocalDateTime.of(2025,9,17,22,0)},
            {4L, 2L, 120, LocalDateTime.of(2025,9,15,17,0)},
            {4L, 3L, 90,  LocalDateTime.of(2025,9,15,19,0)},
            {4L, 2L, 110, LocalDateTime.of(2025,9,16,17,0)},
            {5L, 2L, 60,  LocalDateTime.of(2025,9,15,18,0)},
            {5L, 2L, 55,  LocalDateTime.of(2025,9,16,18,0)},
        };

        List<StudyLogEntity> logs = new ArrayList<>();
        for (Object[] r : data) {
            LocalDateTime start = (LocalDateTime) r[3];
            logs.add(StudyLogEntity.builder()
                    .studentId((Long) r[0])
                    .subjectId((Long) r[1])
                    .durationMinutes((Integer) r[2])
                    .startTime(start)
                    .endTime(start.plusMinutes((Integer) r[2]))
                    .build());
        }
        studyLogRepository.saveAll(logs);
        log.info("[DataInitializer] StudyLog {}개 시드 완료", logs.size());
    }
}
