package com.project200.undabang.exercise.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestConfig;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.querydsl.ExerciseRepositoryCustom;
import com.project200.undabang.exercise.repository.querydsl.impl.ExerciseRepositoryImpl;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberLocation;
import com.project200.undabang.member.enums.MemberGender;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
class ExerciseRepositoryImplTest {
    @Autowired
    private EntityManager em;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @Autowired
    private ExerciseRepository exerciseRepository;

    private ExerciseRepositoryCustom exerciseRepositoryCustom;

    private UUID testUUID = UUID.randomUUID();
    private String testEmail = "e@mail.com";
    private Long testRecordId;

    @BeforeEach
    void setUp(){
        exerciseRepositoryCustom = new ExerciseRepositoryImpl(jpaQueryFactory);

        String testNickname = "테스트닉네임";
        MemberGender testMemberGender = MemberGender.M;
        LocalDate memberBday = LocalDate.of(2010,1,1);

        Member member = Member.builder()
                .memberId(testUUID)
                .memberEmail(testEmail)
                .memberNickname(testNickname)
                .memberGender(testMemberGender)
                .memberBday(memberBday)
                .build();

        em.persist(member);

        Exercise exercise = Exercise.builder()
                .member(member)
                .exerciseTitle("테스트 운동 제목")
                .exerciseDetail("테스트 운동 설명")
                .exercisePersonalType("대충 어떤 운동 종류")
                .exerciseStartedAt(LocalDateTime.now().minusHours(2))
                .exerciseEndedAt(LocalDateTime.now().minusHours(1))
                .exerciseLocation("테스트장소명")
                .build();

        exercise = exerciseRepository.save(exercise);
        testRecordId = exercise.getId();

        Picture picture = Picture.builder()
                .pictureName("테스트 이미지")
                .pictureExtension("jpg")
                .pictureSize(1000)
                .pictureUrl("https://s3-aws/test.jpg")
                .build();

        em.persist(picture);

        Picture picture2 = Picture.builder()
                .pictureName("테스트 이미지2")
                .pictureExtension("jpg")
                .pictureSize(1000)
                .pictureUrl("https://s3-aws/test2.jpg")
                .build();

        em.persist(picture2);

        Picture picture3 = Picture.builder()
                .pictureName("테스트 이미지3")
                .pictureExtension("jpg")
                .pictureSize(1000)
                .pictureUrl("https://s3-aws/test3.jpg")
                .build();

        em.persist(picture3);

        ExercisePicture exercisePicture = ExercisePicture.builder()
                .pictures(picture)
                .exercise(exercise)
                .build();
        em.persist(exercisePicture);

        ExercisePicture exercisePicture2 = ExercisePicture.builder()
                .pictures(picture2)
                .exercise(exercise)
                .build();
        em.persist(exercisePicture2);

        ExercisePicture exercisePicture3 = ExercisePicture.builder()
                .pictures(picture3)
                .exercise(exercise)
                .build();
        em.persist(exercisePicture3);

        // 변경사항 DB에 반영
        em.flush();
        // 영속성 컨텍스트 비우기
        em.clear();
    }

    @Test
    @DisplayName("회원 ID와 운동 기록 ID로 운동 기록 존재 여부 확인 - 성공")
    void existsByRecordIdAndMemberId_Success() {
        // when
        boolean exists = exerciseRepositoryCustom.existsByRecordIdAndMemberId(testUUID, testRecordId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 운동 기록 확인 - 실패")
    void existsByRecordIdAndMemberId_NotExists() {
        // when
        boolean exists = exerciseRepositoryCustom.existsByRecordIdAndMemberId(testUUID, 1234567L);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("다른 회원의 운동 기록 확인 - 실패")
    void existsByRecordIdAndMemberId_WrongMember() {
        // when
        boolean exists = exerciseRepositoryCustom.existsByRecordIdAndMemberId(UUID.randomUUID(), testRecordId);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("운동 기록 상세 조회 - 성공")
    void findExerciseByExerciseId_Success() {
        // when
        FindExerciseRecordResponseDto result = exerciseRepositoryCustom.findExerciseByExerciseId(testUUID, testRecordId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getExerciseTitle()).isEqualTo("테스트 운동 제목");
        assertThat(result.getExerciseDetail()).isEqualTo("테스트 운동 설명");
        assertThat(result.getExercisePersonalType()).isEqualTo("대충 어떤 운동 종류");
        assertThat(result.getExerciseLocation()).isEqualTo("테스트장소명");
        assertThat(result.getExercisePictureUrls()).isPresent();
        assertThat(result.getExercisePictureUrls().get()).contains("https://s3-aws/test.jpg");
        assertThat(result.getExercisePictureUrls().get()).contains("https://s3-aws/test2.jpg");
        assertThat(result.getExercisePictureUrls().get()).contains("https://s3-aws/test3.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 운동 기록 조회 - 실패")
    void findExerciseByExerciseId_NotExists() {
        // when
        FindExerciseRecordResponseDto result = exerciseRepositoryCustom.findExerciseByExerciseId(testUUID, 23402934L);

        // then
        assertThat(result).isNull();
    }
}