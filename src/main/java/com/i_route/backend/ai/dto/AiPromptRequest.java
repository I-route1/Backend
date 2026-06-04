package com.i_route.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptRequest {

    private Long studentId;

    // 🎯 1. 어떤 허깅페이스 모델을 사용할지 지정 (예: "MATH_LLM", "WRITING_AI")
    private String targetModel;

    // 🧠 2. 모델의 역할/성격 부여 (System Prompt)
    // 예: "당신은 학생의 수학적 메타인지를 분석하는 1타 강사입니다."
    private String systemInstruction;

    // 📊 3. 프롬프트에 들어갈 실제 학생 상담 데이터 (DB에서 긁어온 텍스트)
    // 예: "학생 성향: 완벽주의, 최근 수학 점수: 75점, 취약 단원: 함수"
    private String contextData;

    // 💬 4. 모델에게 시킬 구체적인 명령 (User Prompt)
    // 예: "이 학생의 성향과 점수를 바탕으로 앞으로의 수학 학습 가이드를 3줄로 작성해 줘."
    private String userQuery;
}