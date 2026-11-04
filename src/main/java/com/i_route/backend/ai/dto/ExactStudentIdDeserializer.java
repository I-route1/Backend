package com.i_route.backend.ai.dto;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;

public class ExactStudentIdDeserializer extends JsonDeserializer<Long> {
    @Override public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) return parser.getLongValue();
        // 기존 숫자 문자열 ID는 지원하되 소수 ID는 허용하지 않는다.
        if (parser.currentToken() == JsonToken.VALUE_STRING && parser.getText().matches("[0-9]+")) {
            try { return Long.parseLong(parser.getText()); } catch (NumberFormatException ignored) {}
        }
        return context.reportInputMismatch(Long.class, "학생 ID는 정수여야 합니다.");
    }
}
