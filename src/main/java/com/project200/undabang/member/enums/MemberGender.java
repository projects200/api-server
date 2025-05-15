package com.project200.undabang.member.enums;

import lombok.Getter;

@Getter
public enum MemberGender {
    M('m'),
    F('f'),
    U('u');

    private final char code;

    MemberGender(char code){
        this.code = code;
    }

    public static MemberGender fromCode(char code) {
        for (MemberGender gender : MemberGender.values()) {
            if (gender.getCode() == code) {
                return gender;
            }
        }
        throw new IllegalArgumentException("올바른 성별을 입력해주세요 : " + code);
    }
}
