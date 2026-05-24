package com.i_route.backend.ai.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.ai.entity.Question;
import com.i_route.backend.ai.entity.StudyMaterial;
import com.i_route.backend.ai.repository.QuestionRepository;
import com.i_route.backend.ai.repository.StudyMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final StudyMaterialRepository studyMaterialRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedStudyMaterials();
        seedQuestions();
    }

    private void seedStudyMaterials() {
        if (studyMaterialRepository.count() > 0) return;

        List<StudyMaterial> materials = new ArrayList<>();

        // subjectId: 1=국어, 2=수학, 3=영어, 4=과학, 5=사회
        // level: 1=기초 ~ 5=심화
        Object[][] data = {
            // 국어
            {"국어 기초 독해력 워크북", "교재", 1, 1L},
            {"국어 기초 문법 완성", "교재", 2, 1L},
            {"중학 국어 문학 독해", "교재", 3, 1L},
            {"고등 국어 비문학 실전", "교재", 4, 1L},
            {"수능 국어 심화 모의고사", "교재", 5, 1L},
            {"EBS 국어 기초 강좌", "인강", 1, 1L},
            {"국어 개념 완성 강좌", "인강", 2, 1L},
            {"중학 국어 내신 완성", "인강", 3, 1L},
            {"고등 국어 1등급 전략", "인강", 4, 1L},
            {"수능 국어 파이널 강좌", "인강", 5, 1L},
            // 수학
            {"수학 기초 연산 워크북", "교재", 1, 2L},
            {"중학 수학 개념 완성", "교재", 2, 2L},
            {"중학 수학 실력 업그레이드", "교재", 3, 2L},
            {"고등 수학 유형별 공략", "교재", 4, 2L},
            {"수능 수학 심화 문제집", "교재", 5, 2L},
            {"수학 기초 개념 강좌", "인강", 1, 2L},
            {"중학 수학 핵심 정리", "인강", 2, 2L},
            {"고등 수학 개념+유형", "인강", 3, 2L},
            {"수능 수학 1등급 공략", "인강", 4, 2L},
            {"수학 킬러문항 집중 공략", "인강", 5, 2L},
            // 영어
            {"영어 기초 단어 암기장", "교재", 1, 3L},
            {"중학 영어 어법 완성", "교재", 2, 3L},
            {"중학 영어 독해 실전", "교재", 3, 3L},
            {"고등 영어 구문 독해", "교재", 4, 3L},
            {"수능 영어 심화 독해", "교재", 5, 3L},
            {"영어 기초 회화 강좌", "인강", 1, 3L},
            {"중학 영어 내신 대비", "인강", 2, 3L},
            {"고등 영어 독해 전략", "인강", 3, 3L},
            {"수능 영어 1등급 비법", "인강", 4, 3L},
            {"수능 영어 파이널 모의고사", "인강", 5, 3L},
            // 과학
            {"과학 기초 탐구 워크북", "교재", 1, 4L},
            {"중학 과학 개념 완성", "교재", 2, 4L},
            {"중학 과학 실전 문제집", "교재", 3, 4L},
            {"고등 통합과학 핵심 정리", "교재", 4, 4L},
            {"수능 과학탐구 심화 문제집", "교재", 5, 4L},
            {"과학 기초 개념 강좌", "인강", 1, 4L},
            {"중학 과학 핵심 강의", "인강", 2, 4L},
            {"고등 물리/화학 개념 강좌", "인강", 3, 4L},
            {"수능 과탐 1등급 전략", "인강", 4, 4L},
            {"과학탐구 킬러문항 공략", "인강", 5, 4L},
            // 사회
            {"사회 기초 개념 워크북", "교재", 1, 5L},
            {"중학 사회 핵심 정리", "교재", 2, 5L},
            {"중학 역사 실전 문제집", "교재", 3, 5L},
            {"고등 사회문화 개념 완성", "교재", 4, 5L},
            {"수능 사탐 심화 문제집", "교재", 5, 5L},
            {"사회 기초 개념 강좌", "인강", 1, 5L},
            {"중학 사회/역사 핵심 강의", "인강", 2, 5L},
            {"고등 사회문화 1등급 전략", "인강", 3, 5L},
            {"수능 사탐 선택과목 집중", "인강", 4, 5L},
            {"사탐 킬러문항 실전 대비", "인강", 5, 5L},
        };

        for (Object[] row : data) {
            materials.add(StudyMaterial.builder()
                    .title((String) row[0])
                    .materialType((String) row[1])
                    .level((Integer) row[2])
                    .subjectId((Long) row[3])
                    .build());
        }

        studyMaterialRepository.saveAll(materials);
        log.info("[DataInitializer] StudyMaterial {}개 시드 완료", materials.size());
    }

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
}
