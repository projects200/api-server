package com.project200.undabang.member.entity.converter;

import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class MemberGenderConverter implements AttributeConverter<MemberGender, Character> {


    /**
     * 데이터베이스에서 조회된 성별 값을 해당하는 열거형(MemberGender)으로 변환합니다.
     *
     * @param dbData 데이터베이스에서 조회된 성별 값(Character 타입)
     * @return 변환된 MemberGender 열거형 값, dbData가 null인 경우 null을 반환하며, 유효하지 않은 값일 경우 IllegalArgumentException을 발생시킵니다.
     */
    @Override
    public MemberGender convertToEntityAttribute(Character dbData) {
        if (dbData == null) {
            return null;
        }

        return Stream.of(MemberGender.values())
                .filter(g -> g.getCode() == dbData)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(dbData + "는 유효하지 않은 성별 값입니다."));
    }

    /**
     * MemberGender 객체를 데이터베이스의 열(Column) 값으로 변환합니다.
     *
     * @param memberGender 변환할 성별 값을 나타내는 MemberGender 열거형 객체. 값이 null인 경우 데이터베이스에는 null로 저장됩니다.
     * @return 변환된 Character 값. memberGender가 null인 경우 null을 반환합니다.
     */
    @Override
    public Character convertToDatabaseColumn(MemberGender memberGender) {
        return memberGender != null ? memberGender.getCode() : null;
    }

}
