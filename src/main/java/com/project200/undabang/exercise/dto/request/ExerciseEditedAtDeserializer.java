package com.project200.undabang.exercise.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/** 기기 시각의 소수점 절삭이나 문자열 강제 변환으로 편집 순서가 바뀌지 않게 합니다. */
public class ExerciseEditedAtDeserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(Long.class, "editedAt은 UTC epoch 밀리초 정수여야 합니다.");
        }
        return parser.getLongValue();
    }
}
