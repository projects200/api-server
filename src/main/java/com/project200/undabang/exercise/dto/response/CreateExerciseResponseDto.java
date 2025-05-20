package com.project200.undabang.exercise.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateExerciseResponseDto {
    private final List<String> imageUrlList;
}
