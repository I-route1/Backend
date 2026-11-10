package com.i_route.backend.ai.dto;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public class ExactOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {
    @Override public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            try { return OffsetDateTime.parse(parser.getText()); }
            catch (DateTimeParseException ignored) { }
        }
        return context.reportInputMismatch(OffsetDateTime.class, "시간대가 포함된 ISO 측정 시각을 입력해야 합니다.");
    }
}
