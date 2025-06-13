package com.project200.undabang.exercise.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
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
    private PictureRepository pictureRepository;

    @Autowired
    private EntityManager em;

    private UUID memberId = UUID.randomUUID();
    private Exercise exercise;
    private List<Picture> savedPictures;

    @BeforeEach
    void setUp(){
        makeEntities();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("운동기록ID에 관련된 모든 데이터 조회 확인")
    void findAllByExercise_Id() {
        long exerciseId = exercise.getId();

        List<ExercisePicture> exercisePictureList = exercisePictureRepository.findAllByExercise_Id(exerciseId);

        Assertions.assertThat(exercisePictureList)
                .isNotNull()
                .hasSize(3);
    }

    @Test
    @DisplayName("운동기록 없을시 데이터 조회 상황 확인")
    void findAllByExercise_Id_Failed(){
        long exerciseId = 123456789L;

        List<ExercisePicture> list = exercisePictureRepository.findAllByExercise_Id(exerciseId);

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
        savedPictures = pictureRepository.saveAll(List.of(picture, picture2, picture3));

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

        exercisePictureRepository.saveAll(List.of(exercisePicture, exercisePicture2, exercisePicture3));
    }



    @Test
    @DisplayName("countNotDeletedPicturesByExerciseId을 사용해서 서비스 정상 작동되는지 확인")
    public void countNotDeletedPicturesByExerciseId_success(){
        // given
        Assertions.assertThat(exercisePictureRepository.countNotDeletedPicturesByExerciseId(exercise.getId())).isEqualTo(3L);

        // when
        Picture pictureToDelete1 = savedPictures.get(0);
        Picture pictureToDelete2 = savedPictures.get(1);

        pictureToDelete1.softDelete();
        pictureToDelete2.softDelete();

        pictureRepository.save(pictureToDelete1);
        pictureRepository.save(pictureToDelete2);

        em.flush();
        em.clear();

        // 문제가 되는 메소드를 호출
        long countAfterSoftDelete = exercisePictureRepository.countNotDeletedPicturesByExerciseId(exercise.getId());

        // then
        Assertions.assertThat(countAfterSoftDelete).isEqualTo(1L);
    }
}