package com.project200.undabang.exercise.repository.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.exercise.entity.QExercisePicture;
import com.project200.undabang.exercise.repository.ExercisePictureRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExercisePictureRepositoryImpl implements ExercisePictureRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public long countNotDeletedPicturesByExerciseId(Long exerciseId) {
        QExercisePicture exercisePicture = QExercisePicture.exercisePicture;
        QPicture picture = QPicture.picture;

        return jpaQueryFactory.select(exercisePicture.count())
                .from(exercisePicture)
                .join(exercisePicture.picture, picture)
                .where(
                        exercisePicture.exercise.id.eq(exerciseId),
                        picture.pictureDeletedAt.isNull()
                )
                .fetchOne();
    }
}
