package com.project200.undabang.member.enums;

import lombok.Getter;

@Getter
public enum ExerciseSkillLevel {
    BEGINNER("입문자"),
    ROOKIE("초급자"),
    INTERMEDIATE("중급자"),
    ADVANCED("고급자"),
    SKILLED("숙련자"),
    PRO("선출");

    private final String description;

    ExerciseSkillLevel(String description) {
        this.description = description;
    }
}