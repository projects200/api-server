package com.project200.undabang.exercise.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.dto.response.PictureDataResponse;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.impl.ExerciseRepositoryImpl;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ExerciseRepositoryImplTest {
    @Autowired
    private EntityManager em;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @Autowired
    private ExerciseRepository exerciseRepository;

    private ExerciseRepositoryCustom exerciseRepositoryCustom;

    private final UUID testUUID = UUID.randomUUID();
    private final String testEmail = "e@mail.com";
    private Long testRecordId;
    private final LocalDate testDate = LocalDate.of(2025, 5, 1);
    private final LocalDate startDate = LocalDate.of(2025, 5, 1);
    private final LocalDate endDate = LocalDate.of(2025, 5, 2);


    @BeforeEach
    void setUp() {
        exerciseRepositoryCustom = new ExerciseRepositoryImpl(jpaQueryFactory);

        String testNickname = "테스트닉네임";
        MemberGender testMemberGender = MemberGender.M;
        LocalDate memberBday = LocalDate.of(2010, 1, 1);

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
                .exerciseStartedAt(testDate.atTime(10, 0))
                .exerciseEndedAt(testDate.atTime(11, 0))
                .exerciseLocation("테스트장소명")
                .build();

        exercise = exerciseRepository.save(exercise);
        testRecordId = exercise.getId();

        Exercise exercise2 = Exercise.builder()
                .member(member)
                .exerciseTitle("테스트 운동 제목2")
                .exerciseDetail("테스트 운동 설명2")
                .exercisePersonalType("대충 어떤 운동 종류2")
                .exerciseStartedAt(testDate.atTime(4, 0))
                .exerciseEndedAt(testDate.atTime(5, 0))
                .exerciseLocation("테스트장소명")
                .build();

        exerciseRepository.save(exercise2);


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
                .picture(picture)
                .exercise(exercise)
                .build();
        em.persist(exercisePicture);

        ExercisePicture exercisePicture2 = ExercisePicture.builder()
                .picture(picture2)
                .exercise(exercise)
                .build();
        em.persist(exercisePicture2);

        ExercisePicture exercisePicture3 = ExercisePicture.builder()
                .picture(picture3)
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
        assertThat(result.getPictureDataList()).isPresent();

        List<PictureDataResponse> pictureDataList = result.getPictureDataList().get();
        assertThat(pictureDataList).hasSize(3);

        List<String> pictureUrlList = new ArrayList<>();
        for (PictureDataResponse response : pictureDataList) {
            pictureUrlList.add(response.getPictureUrl());
        }

        assertThat(pictureUrlList).contains("https://s3-aws/test.jpg", "https://s3-aws/test2.jpg", "https://s3-aws/test3.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 운동 기록 조회 - 실패")
    void findExerciseByExerciseId_NotExists() {
        // when
        FindExerciseRecordResponseDto result = exerciseRepositoryCustom.findExerciseByExerciseId(testUUID, 23402934L);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("특정 날짜에 해당하는 운동기록 조회_성공")
    void findExerciseRecordByDate_Success() {
        List<FindExerciseRecordDateResponseDto> responseDtoList = exerciseRepositoryCustom.findExerciseRecordByDate(testUUID, testDate);

        Assertions.assertThat(responseDtoList).isNotNull();
        Assertions.assertThat(responseDtoList).isNotEmpty();
        Assertions.assertThat(responseDtoList).hasSize(2);

        FindExerciseRecordDateResponseDto responseDto = responseDtoList.stream()
                .filter(dto -> dto.getExerciseId().equals(testRecordId))
                .findFirst()
                .orElse(null);

        Assertions.assertThat(responseDto).isNotNull();
        Assertions.assertThat(responseDto.getExerciseId()).isEqualTo(testRecordId);
        Assertions.assertThat(responseDto.getExerciseTitle()).isEqualTo("테스트 운동 제목");
        Assertions.assertThat(responseDto.getExercisePersonalType()).isEqualTo("대충 어떤 운동 종류");
        Assertions.assertThat(responseDto.getExerciseStartedAt().toLocalDate()).isEqualTo(testDate);
        Assertions.assertThat(responseDto.getExerciseEndedAt().toLocalDate()).isEqualTo(testDate);
        Assertions.assertThat(responseDto.getPictureUrl()).isNotNull();
        Assertions.assertThat(responseDto.getPictureUrl()).hasSize(3);
        Assertions.assertThat(responseDto.getPictureUrl()).containsExactlyInAnyOrder("https://s3-aws/test.jpg", "https://s3-aws/test2.jpg", "https://s3-aws/test3.jpg");
    }

    @Test
    @DisplayName("특정 날짜에 해당하는 운동 기록 없음")
    void findExerciseRecordByDate_Failed_RecordNotExist() {
        List<FindExerciseRecordDateResponseDto> result = exerciseRepositoryCustom.findExerciseRecordByDate(testUUID, LocalDate.of(2011, 1, 1));

        assertThat(result).isEmpty();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("다른 회원의 특정 날짜 운동기록 조회_실패")
    void findExerciseRecordByDate_WrongMember() {
        UUID randomUUID = UUID.randomUUID();

        List<FindExerciseRecordDateResponseDto> result = exerciseRepositoryCustom.findExerciseRecordByDate(randomUUID, testDate);

        assertThat(result).isEmpty();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("특정 기간동안의 운동기록 조회_성공")
    void findExerciseRecordsByPeriod() {
        // when
        List<FindExerciseRecordByPeriodResponseDto> result = exerciseRepositoryCustom.findExercisesByPeriod(testUUID, startDate, endDate);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).isNotNull();

        long days = ChronoUnit.DAYS.between(startDate, endDate);
        assertThat(result).hasSize((int) days + 1);

        boolean nonZero = false;
        boolean zero = false;

        for (FindExerciseRecordByPeriodResponseDto dto : result) {
            Long count = dto.getExerciseCount();
            if (count > 0) {
                nonZero = true;
            }
            if (count == 0) {
                zero = true;
            }
        }

        assertThat(nonZero).isTrue();
        assertThat(zero).isTrue();
    }
}