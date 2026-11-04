package com.i_route.backend.ai.dto;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;

/** Jackson의 기본 소수→정수 절삭으로 점수/문항 수가 조용히 바뀌는 것을 막는다. */
public class ExactIntegerDeserializer extends JsonDeserializer<Integer> {
    @Override public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) return parser.getIntValue();
        return context.reportInputMismatch(Integer.class, "정수 값을 입력해야 합니다.");
    }
}
