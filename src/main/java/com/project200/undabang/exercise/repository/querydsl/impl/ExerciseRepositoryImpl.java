package com.project200.undabang.exercise.repository.querydsl.impl;

import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.querydsl.ExerciseRepositoryCustom;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class ExerciseRepositoryImpl extends QuerydslRepositorySupport implements ExerciseRepositoryCustom {
    public ExerciseRepositoryImpl(){
        super(Exercise.class);
    }
}
