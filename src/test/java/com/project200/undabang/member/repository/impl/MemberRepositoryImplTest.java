package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.dto.record.MemberProfileRecord;
import com.project200.undabang.member.dto.record.PreferredExerciseRecord;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class MemberRepositoryImplTest {
    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@example.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.MALE)
                .memberBday(LocalDate.of(1995, 5, 5))
                .memberScore((byte) 36)
                .memberDesc("안녕하세요, " + nickname + "입니다.")
                .build();

        em.persist(member);
        return member;
    }

    private Exercise createAndSaveExercise(Member member, LocalDateTime startedAt) {
        Exercise exercise = Exercise.builder()
                .member(member)
                .exerciseTitle("testTitle")
                .exerciseStartedAt(startedAt)
                .exerciseEndedAt(startedAt.plusHours(1))
                .build();
        em.persist(exercise);
        return exercise;
    }

    private ExerciseType createAndSaveExerciseType(String name) {
        ExerciseType type = ExerciseType.builder()
                .exerciseName(name)
                .exerciseTypeImageUrl("http://example.com/" + name + ".png")
                .build();
        em.persist(type);
        return type;
    }

    private void createAndSavePreferredExercise(Member member, ExerciseType type, boolean isDeleted) {
        boolean[] days = new boolean[7];
        days[0] = true;

        PreferredExercise preferred = PreferredExercise.builder()
                .member(member)
                .exercise(type)
                .preferredExerciseSkillLevel(ExerciseSkillLevel.BEGINNER)
                .build();

        preferred.setDaysOfWeek(days);

        if (isDeleted) {
            preferred.delete();
        }

        em.persist(preferred);
    }

    // [수정됨] Member 엔티티에 사진 정보를 업데이트하는 로직 추가
    private void createAndSaveMemberPicture(Member member, String url) {
        // 1. Picture 생성 및 저장
        Picture picture = Picture.builder()
                .pictureUrl(url)
                .build();
        em.persist(picture);

        // 2. MemberPicture 생성 및 저장
        MemberPicture memberPicture = MemberPicture.builder()
                .member(member)
                .picture(picture)
                .memberPicturesUrl(url)
                .build();
        em.persist(memberPicture);

        // 3. [중요] Member 엔티티(연관관계의 주인)에 사진 정보 업데이트
        member.updateProfilePicture(memberPicture);
        // em.persist(member)는 필요 없음 (이미 영속 상태이므로 flush 시점에 Update 쿼리 발생)
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findMemberProfileWithByMemberIdAndPreferredExerciseActive 메소드는")
    class Describe_findMemberProfileWithByMemberIdAndPreferredExerciseActive {

        @Test
        @DisplayName("회원 프로필 정보(사진 포함)와 삭제되지 않은 선호 운동 목록을 조회한다")
        void it_returns_member_profile_with_active_preferred_exercises() {
            // given
            Member member = createAndSaveMember("healthyUser");
            createAndSaveMemberPicture(member, "http://my-profile.com/img.jpg");

            ExerciseType soccer = createAndSaveExerciseType("SOCCER");
            ExerciseType tennis = createAndSaveExerciseType("TENNIS");

            createAndSavePreferredExercise(member, soccer, false);
            createAndSavePreferredExercise(member, tennis, false);

            ExerciseType deletedSport = createAndSaveExerciseType("DELETED_SPORT");
            createAndSavePreferredExercise(member, deletedSport, true);

            flushAndClear();

            // when
            Optional<MemberProfileRecord> result = memberRepository
                    .findMemberProfileWithPreferredExerciseActiveByMemberId(member.getMemberId());

            // then
            assertThat(result).isPresent();
            MemberProfileRecord profile = result.get();

            assertThat(profile.nickname()).isEqualTo("healthyUser");
            assertThat(profile.bio()).isEqualTo("안녕하세요, healthyUser입니다.");
            assertThat(profile.gender()).isEqualTo(MemberGender.MALE);
            assertThat(profile.memberScore()).isEqualTo((byte) 36);

            assertThat(profile.profileImageUrl()).isEqualTo("http://my-profile.com/img.jpg");

            List<PreferredExerciseRecord> exercises = profile.preferredExerciseRecordList();
            assertThat(exercises).hasSize(2);
            assertThat(exercises)
                    .extracting("name")
                    .containsExactlyInAnyOrder("SOCCER", "TENNIS");

            PreferredExerciseRecord soccerRecord = exercises.stream()
                    .filter(e -> e.name().equals("SOCCER"))
                    .findFirst().orElseThrow();
            assertThat(soccerRecord.skillLevel()).isEqualTo(ExerciseSkillLevel.BEGINNER);
            assertThat(soccerRecord.imageUrl()).contains("SOCCER.png");
        }

        @Test
        @DisplayName("선호 운동이 없어도 회원 프로필은 정상 조회되며, 운동 목록은 빈 리스트다")
        void it_returns_profile_with_empty_exercise_list() {
            // given
            Member member = createAndSaveMember("lonelyUser");
            createAndSaveMemberPicture(member, "http://my-profile.com/lonely.jpg");

            flushAndClear();

            // when
            Optional<MemberProfileRecord> result = memberRepository
                    .findMemberProfileWithPreferredExerciseActiveByMemberId(member.getMemberId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().preferredExerciseRecordList()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("프로필 사진이 설정되지 않은 경우 사진 URL은 null이다")
        void it_returns_null_image_url_when_no_picture_set() {
            // given
            Member member = createAndSaveMember("noPicUser");

            ExerciseType gym = createAndSaveExerciseType("GYM");
            createAndSavePreferredExercise(member, gym, false);

            flushAndClear();

            // when
            Optional<MemberProfileRecord> result = memberRepository
                    .findMemberProfileWithPreferredExerciseActiveByMemberId(member.getMemberId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().profileImageUrl()).isNull();
            assertThat(result.get().preferredExerciseRecordList()).hasSize(1);
        }

        @Test
        @DisplayName("탈퇴한(삭제된) 회원은 조회되지 않는다")
        void it_returns_empty_when_member_is_deleted() {
            // given
            Member member = createAndSaveMember("deletedUser");

            org.springframework.test.util.ReflectionTestUtils.setField(member, "memberDeletedAt", LocalDateTime.now());

            em.persist(member);
            flushAndClear();

            // when
            Optional<MemberProfileRecord> result = memberRepository
                    .findMemberProfileWithPreferredExerciseActiveByMemberId(member.getMemberId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID 조회 시 Empty Optional을 반환한다")
        void it_returns_empty_when_id_does_not_exist() {
            // given
            UUID randomId = UUID.randomUUID();

            // when
            Optional<MemberProfileRecord> result = memberRepository
                    .findMemberProfileWithPreferredExerciseActiveByMemberId(randomId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countMemberExerciseInLastDays 메소드는")
    class Describe_countMemberExerciseInLastDays {

        @Test
        @DisplayName("주어진 기간 내의 삭제되지 않은 운동 기록 개수를 정확히 반환한다")
        void it_returns_correct_count_of_non_deleted_exercises_within_period() {
            // given
            Member testMember = createAndSaveMember("testUser");
            Member anotherMember = createAndSaveMember("anotherUser");

            // 기간 내 운동 기록
            createAndSaveExercise(testMember, LocalDateTime.now()); // 오늘
            createAndSaveExercise(testMember, LocalDateTime.now().minusDays(3)); // 3일 전
            createAndSaveExercise(testMember, LocalDateTime.now().minusDays(6)); // 6일 전

            // 기간 밖 운동 기록
            createAndSaveExercise(testMember, LocalDateTime.now().minusDays(8)); // 8일 전

            // 삭제된 운동 기록
            Exercise deletedExercise = createAndSaveExercise(testMember, LocalDateTime.now().minusDays(1));
            deletedExercise.deleteExercise();
            em.persist(deletedExercise);

            // 다른 사용자의 운동 기록
            createAndSaveExercise(anotherMember, LocalDateTime.now().minusDays(2));

            flushAndClear();

            // when
            Long exerciseCount = memberRepository.countMemberExerciseInLastDays(testMember.getMemberId(), 7);

            // then
            assertThat(exerciseCount).isEqualTo(3);
        }

        @Test
        @DisplayName("기간 내에 운동 기록이 없으면 0을 반환한다")
        void it_returns_zero_when_no_exercises_in_period() {
            // given
            Member testMember = createAndSaveMember("testUser");
            createAndSaveExercise(testMember, LocalDateTime.now().minusDays(10));
            flushAndClear();

            // when
            Long exerciseCount = memberRepository.countMemberExerciseInLastDays(testMember.getMemberId(), 7);

            // then
            assertThat(exerciseCount).isEqualTo(0);
        }

        @Test
        @DisplayName("운동 기록이 전혀 없으면 0을 반환한다")
        void it_returns_zero_when_no_exercises_at_all() {
            // given
            Member testMember = createAndSaveMember("testUser");
            flushAndClear();

            // when
            Long exerciseCount = memberRepository.countMemberExerciseInLastDays(testMember.getMemberId(), 30);

            // then
            assertThat(exerciseCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("countMemberExerciseInThisYear 메소드는")
    class Describe_countMemberExerciseInThisYear {

        @Test
        @DisplayName("올해 수행한 운동 일수(중복 제외)를 정확히 반환한다")
        void it_returns_correct_distinct_day_count_in_this_year() {
            // given
            Member testMember = createAndSaveMember("testUser");
            Member anotherMember = createAndSaveMember("anotherUser");
            LocalDateTime now = LocalDateTime.now();

            // 올해 운동 기록 (중복된 날짜 포함)
            createAndSaveExercise(testMember, now); // 오늘
            createAndSaveExercise(testMember, now.minusHours(1)); // 오늘
            createAndSaveExercise(testMember, now.minusDays(1)); // 어제

            // 작년 운동 기록
            createAndSaveExercise(testMember, now.minusYears(1));

            // 삭제된 운동 기록
            Exercise deletedExercise = createAndSaveExercise(testMember, now.minusDays(2));
            deletedExercise.deleteExercise();
            em.persist(deletedExercise);

            // 다른 사용자의 운동 기록
            createAndSaveExercise(anotherMember, now);

            flushAndClear();

            // when
            Long exerciseDays = memberRepository.countMemberExerciseInThisYear(testMember.getMemberId());

            // then
            // 오늘, 어제 -> 2일
            assertThat(exerciseDays).isEqualTo(2);
        }

        @Test
        @DisplayName("올해 운동 기록이 없으면 0을 반환한다")
        void it_returns_zero_when_no_exercises_in_this_year() {
            // given
            Member testMember = createAndSaveMember("testUser");
            createAndSaveExercise(testMember, LocalDateTime.now().minusYears(1));
            flushAndClear();

            // when
            Long exerciseDays = memberRepository.countMemberExerciseInThisYear(testMember.getMemberId());

            // then
            assertThat(exerciseDays).isEqualTo(0);
        }

        @Test
        @DisplayName("운동 기록이 전혀 없으면 0을 반환한다")
        void it_returns_zero_when_no_exercises_at_all() {
            // given
            Member testMember = createAndSaveMember("testUser");
            flushAndClear();

            // when
            Long exerciseDays = memberRepository.countMemberExerciseInThisYear(testMember.getMemberId());

            // then
            assertThat(exerciseDays).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findAllByIdWithPessimisticLock 메소드는")
    class Describe_findAllByIdWithPessimisticLock {

        @Test
        @DisplayName("주어진 회원 ID 목록에 해당하는 회원들을 비관적 잠금과 함께 조회한다")
        void it_returns_members_with_pessimistic_lock() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");
            Member member3 = createAndSaveMember("user3");

            List<UUID> memberIds = List.of(
                    member1.getMemberId(),
                    member2.getMemberId(),
                    member3.getMemberId()
            );

            flushAndClear();

            // when
            List<Member> foundMembers = memberRepository.findAllByIdWithPessimisticLock(memberIds);

            // then
            assertThat(foundMembers).hasSize(3);
            assertThat(foundMembers).extracting("memberId")
                    .containsExactlyInAnyOrderElementsOf(memberIds);
        }

        @Test
        @DisplayName("빈 ID 목록을 전달하면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_empty_ids_provided() {
            // given
            List<UUID> emptyIds = List.of();

            // when
            List<Member> foundMembers = memberRepository.findAllByIdWithPessimisticLock(emptyIds);

            // then
            assertThat(foundMembers).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID는 결과에서 제외된다")
        void it_excludes_non_existent_ids() {
            // given
            Member existingMember = createAndSaveMember("existingUser");
            UUID nonExistentId = UUID.randomUUID();

            List<UUID> memberIds = List.of(
                    existingMember.getMemberId(),
                    nonExistentId
            );

            flushAndClear();

            // when
            List<Member> foundMembers = memberRepository.findAllByIdWithPessimisticLock(memberIds);

            // then
            assertThat(foundMembers).hasSize(1);
            assertThat(foundMembers.get(0).getMemberId()).isEqualTo(existingMember.getMemberId());
        }

        @Test
        @DisplayName("ID 목록 순서와 상관없이 모든 회원을 조회한다")
        void it_returns_all_members_regardless_of_id_order() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            // 역순으로 ID 목록 생성
            List<UUID> memberIds = List.of(
                    member2.getMemberId(),
                    member1.getMemberId()
            );

            flushAndClear();

            // when
            List<Member> foundMembers = memberRepository.findAllByIdWithPessimisticLock(memberIds);

            // then
            assertThat(foundMembers).hasSize(2);
            assertThat(foundMembers).extracting("memberId")
                    .containsExactlyInAnyOrderElementsOf(memberIds);
        }
    }
}