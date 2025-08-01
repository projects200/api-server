package com.project200.undabang.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import lombok.Getter;

import java.util.stream.Stream;

/**
 * 회원의 성별을 나타내는 열거형 클래스입니다.
 * <p>
 * M: 남성을 나타냄
 * F: 여성을 나타냄
 * U: 성별 비공개를 나타냄
 */
@Getter
public enum MemberGender {
    MALE('M'),
    FEMALE('F'),
    UNKNOWN('U');

    private final char code;

    MemberGender(char code) {
        this.code = code;
    }

    /**
     * API 요청에서 성별을 나타내는 코드 값을 받아 MemberGender 열거형으로 변환합니다.
     *
     * @param code 성별을 나타내는 코드 값. 코드 값은 대소문자에 관계없이 유효합니다.
     *             유효한 코드는 'M', 'F', 'U'입니다.
     * @return 코드에 해당하는 MemberGender 열거형 값.
     * 코드 값이 null인 경우 null을 반환합니다.
     * @throws CustomException 유효하지 않은 코드 값이 주어진 경우 예외를 발생시킵니다.
     */
    @JsonCreator
    public static MemberGender fromCode(String code) {
        // 코드가 null인 경우 null을 반환합니다.
        if (code == null) {
            return null;
        }

        // 코드가 비어있거나 공백만 있는 경우 예외를 발생시킵니다.
        if (code.trim().isEmpty()) {
            throw new CustomException(ErrorCode.MEMBER_GENDER_ERROR, "유효하지 않은 성별 코드입니다: " + code);
        }

        // 코드의 첫 글자를 대문자로 변환하여 비교합니다.
        char codeChar = code.toUpperCase().charAt(0);
        return Stream.of(MemberGender.values())
                .filter(c -> c.getCode() == codeChar)
                .findFirst()
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_GENDER_ERROR, "유효하지 않은 성별 코드입니다: " + code));
    }

    public char getCode() {
        return code;
    }
}
