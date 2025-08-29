package com.project200.undabang.member.enums;

import lombok.Getter;

@Getter
public enum ExerciseSkillLevel {
    BEGINNER("입문"),
    ROOKIE("초급"),
    INTERMEDIATE("중급"),
    ADVANCED("고급"),
    SKILLED("숙련자"),
    PRO("선출");

    private final String description;

    ExerciseSkillLevel(String description) {
        this.description = description;
    }
}