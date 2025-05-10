package com.project200.undabang.member.enums;

import lombok.Getter;

@Getter
public enum ExerciseSkillLevel {
    BEGINNER("초보자"),
    NOVICE("입문자"),
    INTERMEDIATE("숙련자"),
    EXPERT("전문가");

    private final String description;

    ExerciseSkillLevel(String description) {
        this.description = description;
    }
}