package com.project200.undabang.member.enums;

import lombok.Getter;

@Getter
public enum MemberGender {
    M("남"),
    F("여"),
    U("비공개");

    private final String description;

    MemberGender(String description){
        this.description = description;
    }
}
