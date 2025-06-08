package com.project200.undabang.member.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.project200.undabang.member.enums.MemberGender;

import java.io.IOException;

public class MemberGenderDeserializer extends JsonDeserializer<MemberGender> {
    @Override
    public MemberGender deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        String value = jsonParser.getValueAsString();
        if(value == null || value.isEmpty()){
            return null;
        }
        char code = Character.toLowerCase(value.charAt(0));
        try {
            return MemberGender.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바른 성별을 입력해주세요: " + value);
        }
    }
}
