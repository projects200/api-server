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

    @DisplayName("countByExercise_Id 가 SoftDelete된 데이터를 포함하여 갯수를 세는 경우 (버그 재현)")
    // 에러 확인
    public void countByExercise_Id_SoftDelete(){
        // === Given: setUp에서 3개의 사진이 정상적으로 추가된 상태 ===
        // 이 시점에서 총 개수는 3개입니다.
        Assertions.assertThat(exercisePictureRepository.countByExercise_Id(exercise.getId())).isEqualTo(3L);


        // === When: 연결된 Picture 중 2개를 soft-delete 처리 ===
        // 1. 삭제할 Picture 엔티티 2개를 가져옵니다.
        Picture pictureToDelete1 = savedPictures.get(0);
        Picture pictureToDelete2 = savedPictures.get(1);

        // 2. Picture 엔티티의 softDelete 메소드를 호출하여 pictureDeletedAt 필드를 업데이트합니다.
        pictureToDelete1.softDelete();
        pictureToDelete2.softDelete();

        // 3. 변경된 Picture 엔티티를 저장합니다.
        pictureRepository.save(pictureToDelete1);
        pictureRepository.save(pictureToDelete2);

        // 4. DB에 변경사항을 반영하고 캐시를 비웁니다.
        em.flush();
        em.clear();

        // 5. 문제가 되는 메소드를 다시 호출합니다.
        long countAfterSoftDelete = exercisePictureRepository.countByExercise_Id(exercise.getId());

        // === Then: 활성 사진 개수는 1개여야 한다 ===
        // 올바른 동작이라면 soft-delete된 Picture와 연결된 ExercisePicture를 제외하고 '1'을 반환해야 합니다.
        // 하지만 현재 countByExercise_Id 쿼리는 Picture 테이블을 보지 않으므로, 여전히 '3'을 반환할 것입니다.
        // 따라서 이 테스트는 `Expected: 1, but was: 3` 오류를 내며 실패합니다. (RED 단계 성공)
        Assertions.assertThat(countAfterSoftDelete).isEqualTo(1L);
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