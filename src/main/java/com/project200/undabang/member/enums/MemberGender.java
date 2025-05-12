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
}
