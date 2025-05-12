package com.project200.undabang.common.converter;

import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MemberGenderConverter implements AttributeConverter<MemberGender, String> {
    @Override
    public MemberGender convertToEntityAttribute(String s) {
        if (s == null || s.isEmpty()) return null;
        char code = s.charAt(0);
        for (MemberGender gender : MemberGender.values()) {
            if (gender.getCode() == code) return gender;
        }
        throw new IllegalArgumentException("Unknown gender code: " + s);
    }

    @Override
    public String convertToDatabaseColumn(MemberGender memberGender) {
        return memberGender != null ? String.valueOf(memberGender.getCode()) : null;
    }
}
