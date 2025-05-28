package com.project200.undabang.exercise.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ExercisePictureRepositoryTest {

    @Autowired
    private ExercisePictureRepository exercisePictureRepository;

    @Autowired
    private EntityManager em;

    private UUID memberId = UUID.randomUUID();
    private Exercise exercise;

    @BeforeEach
    void setUp(){
        makeEntities();
    }

    @Test
    @DisplayName("운동기록ID에 관련된 모든 데이터 조회 확인")
    void findByExercise_Id() {
        long exerciseId = exercise.getId();

        List<ExercisePicture> exercisePictureList = exercisePictureRepository.findByExercise_Id(exerciseId);

        Assertions.assertThat(exercisePictureList)
                .isNotNull()
                .hasSize(3);
    }

    @Test
    @DisplayName("운동기록 없을시 데이터 조회 상황 확인")
    void findByExercise_Id_Failed(){
        long exerciseId = 123456789L;

        List<ExercisePicture> list = exercisePictureRepository.findByExercise_Id(exerciseId);

        Assertions.assertThat(list)
                .isNullOrEmpty();
    }

    private void makeEntities(){
        Member member = Member.builder()
                .memberId(memberId)
                .memberBday(LocalDate.now().minusDays(1))
                .memberGender(MemberGender.M)
                .memberNickname("nickname")
                .memberEmail("email.com")
                .memberDesc("memberDesc")
                .build();

        em.persist(member);

        exercise = Exercise.builder()
                .member(member)
                .exerciseTitle("title")
                .exerciseStartedAt(LocalDateTime.of(2025,5,27,00,00,00))
                .exerciseEndedAt(LocalDateTime.of(2025,5,27,01,00,00))
                .build();

        em.persist(exercise);

        Picture picture = Picture.builder()
                .pictureName("picture")
                .pictureSize(5)
                .pictureUrl("pictureUrl.jpg")
                .pictureExtension(".jpg")
                .build();

        Picture picture2 = Picture.builder()
                .pictureName("picture2")
                .pictureSize(5)
                .pictureUrl("pictureUrl2.jpg")
                .pictureExtension(".jpg")
                .build();

        Picture picture3 = Picture.builder()
                .pictureName("picture3")
                .pictureSize(5)
                .pictureUrl("pictureUrl3.jpg")
                .pictureExtension(".jpg")
                .build();

        em.persist(picture);
        em.persist(picture2);
        em.persist(picture3);

        ExercisePicture exercisePicture = ExercisePicture.builder()
                .exercise(exercise)
                .picture(picture)
                .build();

        ExercisePicture exercisePicture2 = ExercisePicture.builder()
                .exercise(exercise)
                .picture(picture2)
                .build();

        ExercisePicture exercisePicture3 = ExercisePicture.builder()
                .exercise(exercise)
                .picture(picture3)
                .build();

        exercisePictureRepository.save(exercisePicture);
        exercisePictureRepository.save(exercisePicture2);
        exercisePictureRepository.save(exercisePicture3);
    }
}