package com.project200.undabang.exercise.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DataJpaTest
@Import(TestQuerydslConfig.class)
@DisplayName("ExerciseRepositoryImpl 테스트")
class ExerciseRepositoryImplTest {

    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private EntityManager em;

    /**
     * 테스트용 Member 엔티티를 생성하고 영속화합니다.
     *
     * @param nickname 테스트용 회원 닉네임 (이메일에도 사용)
     * @return 영속화된 Member 객체
     */
    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname)
                .memberNickname(nickname + "@email.com")
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(2010, 1, 1))
                .build();
        return memberRepository.save(member);
    }

    /**
     * 테스트용 Exercise 엔티티를 생성하고 저장합니다.
     *
     * @param member    운동 기록에 연결할 Member 객체
     * @param title     운동 제목
     * @param startedAt 운동 시작 시간
     * @param endedAt   운동 종료 시간
     * @return 저장된 Exercise 객체
     */
    private Exercise createAndSaveExercise(Member member, String title, LocalDateTime startedAt, LocalDateTime endedAt) {
        Exercise exercise = Exercise.builder()
                .member(member)
                .exerciseTitle(title)
                .exerciseDetail("exercise detail for " + title)
                .exercisePersonalType("exercise personal type for " + title)
                .exerciseStartedAt(startedAt)
                .exerciseEndedAt(endedAt)
                .exerciseLocation("exercise location for " + title)
                .build();
        return exerciseRepository.save(exercise);
    }

    /**
     * Picture 엔티티를 생성하고 영속화합니다.
     */
    private Picture createAndPersistPicture(String name, String url, String extension) {
        Picture picture = Picture.builder()
                .pictureName(name)
                .pictureExtension(extension)
                .pictureSize(1000)
                .pictureUrl(url)
                .build();
        em.persist(picture);
        return picture;
    }

    /**
     * ExercisePicture 엔티티(연관 관계)를 생성하고 영속화합니다.
     */
    private ExercisePicture createAndPersistExercisePicture(Exercise exercise, Picture picture) {
        ExercisePicture exercisePicture = ExercisePicture.builder()
                .picture(picture)
                .exercise(exercise)
                .build();
        em.persist(exercisePicture);
        return exercisePicture;
    }

    /**
     * Exercise 엔티티에 Picture 엔티티를 연결하여 ExercisePicture 엔티티를 생성하고 영속화합니다.
     */
    private void createAndPersistExercisePicture(Exercise exercise, String name, String url, String extension) {
        // 이름과 URL을 사용하여 Picture 엔티티를 생성하고 영속화한 후,
        // ExercisePicture 엔티티를 생성하고 영속화합니다.
        Picture picture = createAndPersistPicture(name, url, extension);
        createAndPersistExercisePicture(exercise, picture);
    }

    /**
     * 영속성 컨텍스트의 변경사항을 DB에 즉시 반영(flush)하고,
     * 컨텍스트를 비워(clear) 다음 조회 시 DB에서 데이터를 가져오도록 보장합니다.
     */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }


    // --- 헬퍼 메서드: 데이터 생성을 위한 재사용 로직 ---

    @Nested
    @DisplayName("existsByRecordIdAndMemberId 메서드는")
    class Describe_existsByRecordIdAndMemberId {

        @Test
        @DisplayName("자신의 운동 기록 ID를 조회하면 true를 반환한다")
        void it_returns_true_for_own_record() {
            // given
            Member testMember = createAndSaveMember("testMember");
            LocalDateTime testStartedAt = LocalDateTime.of(2025, 7, 25, 8, 42);
            Exercise testExercise = createAndSaveExercise(testMember, "testExercise", testStartedAt, testStartedAt.plusHours(1));
            flushAndClear();

            // when
            boolean exists = exerciseRepository.existsByRecordIdAndMemberId(testMember.getMemberId(), testExercise.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 운동 기록 ID를 조회하면 false를 반환한다")
        void it_returns_false_for_non_existent_record() {
            // given
            Long nonExistentRecordId = 9999L;
            Member testMember = createAndSaveMember("testMember");
            flushAndClear();

            // when
            boolean exists = exerciseRepository.existsByRecordIdAndMemberId(testMember.getMemberId(), nonExistentRecordId);

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("다른 회원의 운동 기록 ID를 조회하면 false를 반환한다")
        void it_returns_false_for_another_members_record() {
            // given
            Member testMember = createAndSaveMember("testMember");
            LocalDateTime testStartedAt = LocalDateTime.of(2025, 7, 25, 8, 42);
            Exercise testExercise = createAndSaveExercise(testMember, "testExercise", testStartedAt, testStartedAt.plusHours(1));
            flushAndClear();

            // when
            UUID anotherMemberId = UUID.randomUUID();
            boolean exists = exerciseRepository.existsByRecordIdAndMemberId(anotherMemberId, testExercise.getId());

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findExerciseByExerciseId 메서드는")
    class Describe_findExerciseByExerciseId {

        @Test
        @DisplayName("사진이 있는 운동 기록을 조회하면 모든 정보를 포함하여 반환한다")
        void it_returns_a_record_with_all_details_and_pictures() {
            // given
            Member testMember = createAndSaveMember("testMember");
            LocalDateTime testStartedAt = LocalDateTime.of(2025, 7, 25, 8, 42);
            Exercise testExercise = createAndSaveExercise(testMember, "testExercise", testStartedAt, testStartedAt.plusHours(1));
            createAndPersistExercisePicture(testExercise, "test_image_1.jpg", "https://s3-aws/test_image_1.jpg", "jpg");
            createAndPersistExercisePicture(testExercise, "test_image_2.png", "https://s3-aws/test_image_2.png", "png");
            flushAndClear();

            // when
            FindExerciseRecordResponseDto result = exerciseRepository.findExerciseByExerciseId(testMember.getMemberId(), testExercise.getId());

            // then
            assertThat(result).isNotNull();
            assertSoftly(softly -> {
                softly.assertThat(result.getExerciseTitle()).isEqualTo(testExercise.getExerciseTitle());
                softly.assertThat(result.getExerciseDetail()).isEqualTo(testExercise.getExerciseDetail());
                softly.assertThat(result.getExercisePersonalType()).isEqualTo(testExercise.getExercisePersonalType());
                softly.assertThat(result.getExerciseLocation()).isEqualTo(testExercise.getExerciseLocation());
                softly.assertThat(result.getExerciseStartedAt()).isEqualTo(testExercise.getExerciseStartedAt());
                softly.assertThat(result.getExerciseEndedAt()).isEqualTo(testExercise.getExerciseEndedAt());
            });
            assertThat(result.getPictureDataList()).isPresent().hasValueSatisfying(pictures ->
                    assertThat(pictures)
                            .hasSize(2)
                            .extracting("pictureUrl")
                            .containsExactlyInAnyOrder("https://s3-aws/test_image_1.jpg", "https://s3-aws/test_image_2.png")
            );
        }

        @Test
        @DisplayName("사진이 없는 운동 기록을 조회하면 pictureDataList가 Optional.empty()로 반환된다")
        void it_returns_optional_empty_for_pictureDataList_when_no_pictures_exist() {
            // given
            // 운동 기록은 생성하지만, 사진 데이터는 생성하지 않습니다.
            Member testMember = createAndSaveMember("testMemberWithNoPics");
            Exercise exerciseWithoutPictures = createAndSaveExercise(testMember, "사진 없는 운동", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
            flushAndClear();

            // when
            // 해당 운동 기록을 조회합니다.
            FindExerciseRecordResponseDto result = exerciseRepository.findExerciseByExerciseId(
                    testMember.getMemberId(),
                    exerciseWithoutPictures.getId()
            );

            // then
            // 결과는 존재하지만, pictureDataList는 비어있어야 합니다.
            assertThat(result).isNotNull();
            assertThat(result.getExerciseTitle()).isEqualTo("사진 없는 운동");

            // pictureDataList가 Optional.empty()인지 확인합니다.
            assertThat(result.getPictureDataList()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 운동 기록을 조회하면 null을 반환한다")
        void it_returns_null_for_a_non_existent_record() {
            // when
            Member testMember = createAndSaveMember("testMember");
            FindExerciseRecordResponseDto result = exerciseRepository.findExerciseByExerciseId(testMember.getMemberId(), 9999L);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findExerciseRecordByDate 메서드는")
    class Describe_findExerciseRecordByDate {

        @Test
        @DisplayName("특정 날짜에 운동 기록이 있으면 해당 기록을 반환한다")
        void it_returns_records_for_a_specific_date() {
            // given
            Member testMember = createAndSaveMember("testMember");
            LocalDate testExerciseDate = LocalDate.of(2025, 7, 25);
            Exercise testExercise1 = createAndSaveExercise(testMember, "testExercise1",
                    testExerciseDate.atTime(3, 0), testExerciseDate.atTime(4, 0));
            Exercise testExercise2 = createAndSaveExercise(testMember, "testExercise2",
                    testExerciseDate.atTime(6, 0), testExerciseDate.atTime(8, 0));
            createAndPersistExercisePicture(testExercise1, "test_image_1.jpg", "https://s3-aws/test_image_1.jpg", "jpg");
            createAndPersistExercisePicture(testExercise2, "test_image_2.png", "https://s3-aws/test_image_2.png", "png");
            flushAndClear();

            // when
            List<FindExerciseRecordDateResponseDto> responseDtoList =
                    exerciseRepository.findExerciseRecordByDate(testMember.getMemberId(), testExerciseDate);

            // then
            assertThat(responseDtoList).isNotNull();
            assertThat(responseDtoList).isNotEmpty();
            assertThat(responseDtoList).hasSize(2);

            assertSoftly(softly -> {
                softly.assertThat(responseDtoList.getFirst().getExerciseTitle()).isEqualTo(testExercise1.getExerciseTitle());
                softly.assertThat(responseDtoList.getFirst().getExerciseStartedAt()).isEqualTo(testExercise1.getExerciseStartedAt());
                softly.assertThat(responseDtoList.getFirst().getExerciseEndedAt()).isEqualTo(testExercise1.getExerciseEndedAt());
                softly.assertThat(responseDtoList.getFirst().getPictureUrl()).containsExactly("https://s3-aws/test_image_1.jpg");

                softly.assertThat(responseDtoList.get(1).getExerciseTitle()).isEqualTo(testExercise2.getExerciseTitle());
                softly.assertThat(responseDtoList.get(1).getExerciseStartedAt()).isEqualTo(testExercise2.getExerciseStartedAt());
                softly.assertThat(responseDtoList.get(1).getExerciseEndedAt()).isEqualTo(testExercise2.getExerciseEndedAt());
                softly.assertThat(responseDtoList.get(1).getPictureUrl()).containsExactly("https://s3-aws/test_image_2.png");
            });
        }

        @Test
        @DisplayName("특정 날짜에 운동 기록이 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_for_no_records_on_date() {
            // given
            Member testMember = createAndSaveMember("testMember");
            LocalDate testExerciseDate = LocalDate.of(2025, 7, 25);
            flushAndClear();

            // when
            List<FindExerciseRecordDateResponseDto> result =
                    exerciseRepository.findExerciseRecordByDate(testMember.getMemberId(), testExerciseDate);

            // then
            assertThat(result).isEmpty();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("다른 회원의 운동 기록을 조회하면 빈 리스트를 반환한다")
        void it_returns_empty_list_for_another_members_record() {
            // given
            Member testMember = createAndSaveMember("testMember");
            LocalDate testExerciseDate = LocalDate.of(2025, 7, 25);
            Exercise testExercise = createAndSaveExercise(testMember, "testExercise",
                    testExerciseDate.atTime(3, 0), testExerciseDate.atTime(4, 0));
            createAndPersistExercisePicture(testExercise, "test_image_1.jpg", "https://s3-aws/test_image_1.jpg", "jpg");
            flushAndClear();

            // when
            UUID anotherMemberId = UUID.randomUUID();
            List<FindExerciseRecordDateResponseDto> responseDtoList =
                    exerciseRepository.findExerciseRecordByDate(anotherMemberId, testExerciseDate);

            // then
            assertThat(responseDtoList).isNotNull();
            assertThat(responseDtoList).isEmpty();
        }

        @Test
        @DisplayName("운동 기록에 사진이 없으면 pictureUrl이 빈 리스트로 반환된다")
        void it_returns_record_with_empty_picture_list_when_no_pictures_exist() {
            // given
            // 운동 기록은 생성하지만, 사진 데이터는 생성하지 않습니다.
            Member testMember = createAndSaveMember("testMember");
            LocalDate testExerciseDate = LocalDate.of(2025, 7, 25);
            createAndSaveExercise(testMember, "사진 없는 운동", testExerciseDate.atTime(10, 0), testExerciseDate.atTime(11, 0));
            flushAndClear();

            // when
            List<FindExerciseRecordDateResponseDto> responseDtoList =
                    exerciseRepository.findExerciseRecordByDate(testMember.getMemberId(), testExerciseDate);

            // then
            assertThat(responseDtoList).hasSize(1);
            FindExerciseRecordDateResponseDto result = responseDtoList.getFirst();
            assertThat(result.getExerciseTitle()).isEqualTo("사진 없는 운동");

            // 사진이 없으므로 pictureUrl 리스트가 비어있는지 확인합니다.
            assertThat(result.getPictureUrl()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("findExercisesByPeriod 메서드는")
    class Describe_findExercisesByPeriod {

        @Test
        @DisplayName("특정 기간 동안에 운동 기록이 있으면 해당 기록들을 반환한다")
        void it_returns_records_for_a_specific_period() {
            // given
            Member testMember = createAndSaveMember("testMember");
            LocalDate startDate = LocalDate.of(2025, 7, 1);
            LocalDate endDate = LocalDate.of(2025, 7, 31);
            Exercise testExercise1 = createAndSaveExercise(testMember, "testExercise1",
                    startDate.plusDays(5).atStartOfDay(), startDate.plusDays(5).atStartOfDay().plusHours(1));
            Exercise testExercise2 = createAndSaveExercise(testMember, "testExercise2",
                    startDate.plusDays(15).atStartOfDay(), startDate.plusDays(15).atStartOfDay().plusHours(2));
            createAndPersistExercisePicture(testExercise1, "test_image_1.jpg", "https://s3-aws/test_image_1.jpg", "jpg");
            createAndPersistExercisePicture(testExercise2, "test_image_2.png", "https://s3-aws/test_image_2.png", "png");
            flushAndClear();

            // when
            List<FindExerciseRecordByPeriodResponseDto> result =
                    exerciseRepository.findExercisesByPeriod(testMember.getMemberId(), startDate, endDate);

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

    @Nested
    @DisplayName("countByMemberAndExerciseStartedAt 메서드는")
    class Describe_countByMemberAndExerciseStartedAt {

        @Test
        @DisplayName("특정 날짜에 생성된 운동 기록의 수를 정확히 반환한다")
        void it_returns_accurate_count_for_a_specific_date() {
            // given

            // 두 회원 생성
            Member testMember1 = createAndSaveMember("testMember1");
            Member testMember2 = createAndSaveMember("testMember2");

            // 대상 날짜 설정
            LocalDate targetDate = LocalDate.of(2025, 7, 24);

            // testMember1의 운동 기록
            createAndSaveExercise(testMember1, "testExercise1",
                    targetDate.atTime(10, 0), targetDate.atTime(11, 0)); // 대상
            createAndSaveExercise(testMember1, "testExercise2",
                    targetDate.atTime(18, 0), targetDate.atTime(19, 0)); // 대상
            createAndSaveExercise(testMember1, "testExercise3",
                    targetDate.minusDays(1).atTime(10, 0),
                    targetDate.minusDays(1).atTime(11, 0)); // 다른 날짜

            // testMember2의 운동 기록
            createAndSaveExercise(testMember2, "testExercise4",
                    targetDate.atTime(11, 0), targetDate.atTime(12, 0)); // 다른 회원

            flushAndClear();

            // when
            long count = exerciseRepository.countByMemberAndExerciseStartedAt(testMember1, targetDate);

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("삭제된 운동 기록은 개수에서 제외한다")
        void it_excludes_deleted_records_from_the_count() {
            // given
            Member testMember1 = createAndSaveMember("testMember1");
            LocalDate targetDate = LocalDate.of(2025, 7, 24);
            createAndSaveExercise(testMember1, "testExercise1",
                    targetDate.atTime(9, 0), targetDate.atTime(10, 0)); // 정상 기록

            Exercise deletedExercise = createAndSaveExercise(testMember1, "testExercise2",
                    targetDate.atTime(15, 0), targetDate.atTime(16, 0)); // 삭제 대상
            deletedExercise.deleteExercise(); // 삭제 처리
            flushAndClear();

            // when
            long count = exerciseRepository.countByMemberAndExerciseStartedAt(testMember1, targetDate);

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("운동 기록이 없는 날짜에는 0을 반환한다")
        void it_returns_zero_for_a_date_with_no_records() {
            // given
            Member testMember1 = createAndSaveMember("testMember1");
            LocalDate targetDate = LocalDate.of(2025, 7, 24);
            createAndSaveExercise(testMember1, "testExercise1",
                    targetDate.plusDays(1).atTime(10, 0),
                    targetDate.plusDays(1).atTime(11, 0)); // 다른 날짜에만 기록
            flushAndClear();

            // when
            long count = exerciseRepository.countByMemberAndExerciseStartedAt(testMember1, targetDate);

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("findExerciseCountsByDateBetween 메서드는")
    class Describe_findExerciseCountsByDateBetween {

        @Test
        @DisplayName("주어진 기간 동안의 날짜별 운동 기록 수를 Map 형태로 정확히 반환한다")
        void it_returns_a_map_of_daily_counts_within_the_period() {
            // given

            // 두 회원 생성
            Member testMember1 = createAndSaveMember("testMember1");
            Member testMember2 = createAndSaveMember("testMember2");

            // 기간 설정
            LocalDate startDate = LocalDate.of(2025, 7, 20);
            LocalDate endDate = LocalDate.of(2025, 7, 24);

            // testMember1의 기록
            createAndSaveExercise(testMember1, "testExercise1",
                    startDate.atTime(10, 0), startDate.atTime(11, 0)); // 7/20 1개
            createAndSaveExercise(testMember1, "testExercise2",
                    startDate.plusDays(1).atTime(11, 0),
                    startDate.plusDays(1).atTime(12, 0)); // 7/21 1개
            createAndSaveExercise(testMember1, "testExercise3",
                    startDate.plusDays(1).atTime(12, 0),
                    startDate.plusDays(1).atTime(13, 0)); // 7/21 2개
            createAndSaveExercise(testMember1, "testExercise4",
                    endDate.atTime(23, 59), endDate.plusDays(1).atTime(0, 59)); // 7/24 1개

            // 경계값 및 범위 밖 데이터
            createAndSaveExercise(testMember1, "testExercise5",
                    startDate.minusDays(1).atTime(10, 0),
                    startDate.minusDays(1).atTime(11, 0)); // 범위 밖
            createAndSaveExercise(testMember1, "testExercise6",
                    endDate.plusDays(1).atTime(10, 0),
                    endDate.plusDays(1).atTime(11, 0)); // 범위 밖
            createAndSaveExercise(testMember2, "testExercise7",
                    startDate.plusDays(1).atTime(11, 0),
                    startDate.plusDays(1).atTime(12, 0)); // 다른 회원

            flushAndClear();

            // when
            Map<LocalDate, Long> result = exerciseRepository.findExerciseCountsByDateBetween(testMember1, startDate, endDate);

            // then
            assertThat(result).hasSize(3) // 7/22, 7/23은 기록이 없으므로 사이즈는 3
                    .containsEntry(LocalDate.of(2025, 7, 20), 1L)
                    .containsEntry(LocalDate.of(2025, 7, 21), 2L)
                    .containsEntry(LocalDate.of(2025, 7, 24), 1L);
        }

        @Test
        @DisplayName("삭제된 기록은 결과에서 제외한다")
        void it_excludes_deleted_records_from_the_result() {
            // given
            Member testMember1 = createAndSaveMember("testMember1");

            LocalDate startDate = LocalDate.of(2025, 7, 20);
            LocalDate endDate = LocalDate.of(2025, 7, 22);

            createAndSaveExercise(testMember1, "testExercise1",
                    startDate.atTime(10, 0), startDate.atTime(11, 0)); // 정상 기록
            Exercise deletedExercise = createAndSaveExercise(testMember1, "testExercise2",
                    startDate.atTime(11, 0), startDate.atTime(12, 0)); // 삭제 대상
            deletedExercise.deleteExercise(); // 삭제 처리

            createAndSaveExercise(testMember1, "testExercise3",
                    endDate.atTime(10, 0), endDate.atTime(11, 0)); // 다른 날짜 정상 기록

            flushAndClear();

            // when
            Map<LocalDate, Long> result = exerciseRepository.findExerciseCountsByDateBetween(testMember1, startDate, endDate);

            // then
            assertThat(result).hasSize(2)
                    .containsEntry(startDate, 1L) // 삭제된 기록이 제외되어 1개
                    .containsEntry(endDate, 1L);
        }

        @Test
        @DisplayName("주어진 기간에 운동 기록이 없으면 빈 Map을 반환한다")
        void it_returns_an_empty_map_if_no_records_in_period() {
            // given
            Member testMember1 = createAndSaveMember("testMember1");

            LocalDate startDate = LocalDate.of(2025, 7, 20);
            LocalDate endDate = LocalDate.of(2025, 7, 24);

            createAndSaveExercise(testMember1, "testExercise1",
                    startDate.minusDays(1).atTime(10, 0),
                    startDate.minusDays(1).atTime(11, 0)); // 범위 밖
            createAndSaveExercise(testMember1, "testExercise2",
                    endDate.plusDays(1).atTime(10, 0),
                    endDate.plusDays(1).atTime(11, 0)); // 범위 밖
            flushAndClear();

            // when
            Map<LocalDate, Long> result = exerciseRepository.findExerciseCountsByDateBetween(testMember1, startDate, endDate);

            // then
            assertThat(result).isEmpty();
        }
    }
}