package com.project200.undabang.member.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.dto.command.SignUpMemberCommand;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private UUID testUUID;

    @BeforeEach
    void setUp() {
        testUUID = UUID.randomUUID();

        Member member = Member.signUp(
                SignUpMemberCommand.builder()
                        .memberId(testUUID)
                        .memberEmail("user@email.com")
                        .memberNickname("유저닉네임")
                        .memberGender(MemberGender.MALE)
                        .initialSignupPoints((byte) 35)
                        .memberBday(LocalDate.of(1990, 1, 1))
                        .build());

        memberRepository.save(member);
    }

    @Test
    @DisplayName("이메일이 이미 존재하는 회원의 경우")
    void existsByEmail_exists() {
        String email = "user@email.com";
        boolean check = memberRepository.existsByMemberEmail(email);

        assertTrue(check);
    }

    @Test
    @DisplayName("이메일이 존재하지 않는 회원의 경우")
    void existsByEmail_not_exists() {
        String email = "user@gmail.com";
        boolean check = memberRepository.existsByMemberEmail(email);

        assertFalse(check);
    }

    @Test
    @DisplayName("이미 존재하는 닉네임을 입력한 회원의 경우")
    void existsByMemberNickname_exists() {
        String name = "유저닉네임";
        boolean check = memberRepository.existsByMemberNickname(name);
        assertTrue(check);
    }

    @Test
    @DisplayName("닉네임을 중복없이 입력한 회원의 경우")
    void existsByMemberNickname_not_exists() {
        String name = "테스트유저닉네임";
        boolean check = memberRepository.existsByMemberNickname(name);
        assertFalse(check);
    }

    /**
     * 회원의 ID가 데이터베이스에 존재하는 경우를 테스트합니다.
     */
    @Test
    @DisplayName("회원 ID가 존재하는 경우")
    void existsByMemberId_exists() {
        // given
        UUID existingMemberId = testUUID;

        // when
        boolean result = memberRepository.existsByMemberId(existingMemberId);

        // then
        assertThat(result).isTrue();
    }

    /**
     * 회원의 ID가 데이터베이스에 존재하지 않는 경우를 테스트합니다.
     */
    @Test
    @DisplayName("회원 ID가 존재하지 않는 경우")
    void existsByMemberId_not_exists() {
        // given
        UUID nonExistingMemberId = UUID.randomUUID();

        // when
        boolean result = memberRepository.existsByMemberId(nonExistingMemberId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("회원의 운동점수 조회")
    void findMemberScore_Success() {
        // given
        UUID memberId = testUUID;

        // when
        Optional<Member> foundMember = memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId);

        // then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getMemberDeletedAt()).isNull();
    }

    @Test
    @DisplayName("회원의 운동점수 조회 _ 탈퇴한 경우")
    void findMemberScore_WithdrawnMember() {
        UUID testMemberId = UUID.randomUUID();
        Member withdrawnMember = Member.builder()
                .memberId(testMemberId)
                .memberEmail("e@eail.com")
                .memberNickname("탈퇴유저닉테임")
                .memberDeletedAt(LocalDateTime.now()) // 빌더를 통해 탈퇴 시간 설정
                .build();
        memberRepository.save(withdrawnMember);

        // when
        Optional<Member> foundMember = memberRepository.findByMemberIdAndMemberDeletedAtNull(testMemberId);

        // then
        assertThat(foundMember).isNotPresent();
    }

    @Test
    @DisplayName("회원의 운동점수 조회 _ 회원이 없는 경우")
    void findMemberScore_NonExistentMember() {
        // given
        UUID nonExistentMemberId = UUID.randomUUID();

        // when
        Optional<Member> foundMember = memberRepository.findByMemberIdAndMemberDeletedAtNull(nonExistentMemberId);

        // then
        assertThat(foundMember).isNotPresent();
    }

    @Test
    @DisplayName("성별 컨버터가 정상적으로 동작하여 멤버를 조회한다")
    void findById_Success() {
        // given
        em.flush();
        em.clear();

        // when
        Optional<Member> foundMemberOptional = memberRepository.findById(testUUID);

        // then
        assertThat(foundMemberOptional).isPresent();
        Member foundMember = foundMemberOptional.get();
        assertThat(foundMember.getMemberGender()).isEqualTo(MemberGender.MALE);
    }

    @Test
    @DisplayName("성별이 null인 멤버를 조회한다")
    void findById_NullGender() {
        // given
        UUID nullGenderMemberId = UUID.randomUUID();
        Member nullGenderMember = Member.builder()
                .memberId(nullGenderMemberId)
                .memberEmail("")
                .memberNickname("nullGenderUser")
                .memberGender(null) // 성별을 null로 설정
                .memberBday(LocalDate.of(1990, 1, 1))
                .build();
        memberRepository.save(nullGenderMember);
        em.flush();
        em.clear();

        // when
        Optional<Member> foundMemberOptional = memberRepository.findById(nullGenderMemberId);

        // then
        assertThat(foundMemberOptional).isPresent();
        Member foundMember = foundMemberOptional.get();
        assertThat(foundMember.getMemberGender()).isNull(); // 성별이 null이어야 함
        assertThat(foundMember.getMemberId()).isEqualTo(nullGenderMemberId);
    }

    @Test
    @DisplayName("회원의 성별이 M, F, U 중 하나가 아닌 경우를 조회한다")
    void findById_InvalidGender() {
        // given
        UUID invalidGenderMemberId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO members (member_id, member_email, member_nickname, member_gender, member_bday, member_score) " +
                        "VALUES (?, ?, ?, 'X', ?, ?)")
                .setParameter(1, invalidGenderMemberId)
                .setParameter(2, "invalid@email.com")
                .setParameter(3, "invalidGenderUser")
                .setParameter(4, LocalDate.of(1990, 1, 1))
                .setParameter(5, (byte) 0)
                .executeUpdate();

        em.flush();
        em.clear();

        // when & then
        assertThatThrownBy(() -> memberRepository.findById(invalidGenderMemberId))
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X는 유효하지 않은 성별 값입니다.");
    }

    private Member createAndSaveMember(String nickname, String email, boolean isDeleted) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(email)
                .memberNickname(nickname)
                .memberGender(MemberGender.MALE)
                .memberBday(LocalDate.of(1995, 5, 10))
                .memberScore((byte) 50)
                .memberDeletedAt(isDeleted ? LocalDateTime.now() : null)
                .build();
        em.persist(member);
        return member;
    }

    // ============== 테스트 헬퍼 메소드 ==============

    private Picture createAndSavePicture(Member member) {
        Picture picture = Picture.builder()
                .pictureName(member.getMemberNickname() + "_profile.jpg")
                .pictureExtension(".jpg")
                .pictureSize(1024)
                .pictureUrl("http://example.com/pictures/" + member.getMemberId())
                .build();
        em.persist(picture);
        return picture;
    }

    private MemberPicture createAndSaveMemberPicture(Member member) {
        Picture picture = createAndSavePicture(member);
        MemberPicture memberPicture = MemberPicture.builder()
                .id(picture.getId())
                .picture(picture)
                .member(member)
                .build();
        em.persist(memberPicture);
        return memberPicture;
    }

    private ExerciseType createAndSavePreferredExercise() {
        ExerciseType exerciseType = ExerciseType.builder()
                .exerciseName("헬스")
                .exerciseTypeImageUrl("http://example.com/exercise/weight_training.jpg")
                .build();
        em.persist(exerciseType);
        return exerciseType;
    }

    private PreferredExercise createAndSavePreferredExercise(Member member) {
        ExerciseType exerciseType = createAndSavePreferredExercise();
        PreferredExercise preferredExercise = PreferredExercise.builder()
                .member(member)
                .exercise(exerciseType)
                .build();
        em.persist(preferredExercise);
        return preferredExercise;
    }

    @Nested
    @DisplayName("findMemberProfileByMemberIdAndMemberDeletedAtNull 메소드는")
    class FindMemberProfileByMemberIdAndMemberDeletedAtNull {
        @Test
        @DisplayName("정상적인 회원 ID로 조회 시, 삭제되지 않은 회원의 프로필 정보를 반환한다")
//        @Transactional
        void findMemberProfileByMemberIdAndMemberDeletedAtNull_success() {
            // given
            Member member = createAndSaveMember("testuser", "test@example.com", false);
            MemberPicture memberPicture = createAndSaveMemberPicture(member);
            PreferredExercise preferredExercise = createAndSavePreferredExercise(member);
            member.updateProfilePicture(memberPicture);

            em.flush();
            em.clear();

            // when
            Optional<Member> foundMemberOptional = memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(member.getMemberId());

            // then
            assertThat(foundMemberOptional).as("회원 프로필 조회가 성공해야 합니다.").isPresent();
            Member foundMember = foundMemberOptional.get();

            assertSoftly(softly -> {
                softly.assertThat(foundMember.getMemberId()).as("조회된 회원의 ID가 일치해야 합니다.").isEqualTo(member.getMemberId());
                softly.assertThat(foundMember.getMemberNickname()).as("조회된 회원의 닉네임이 일치해야 합니다.").isEqualTo("testuser");
                softly.assertThat(foundMember.getMemberPicture()).as("회원의 사진 정보가 로드되어야 합니다.").isNotNull();
                softly.assertThat(foundMember.getMemberPicture().getId()).as("회원 사진 ID가 일치해야 합니다.").isEqualTo(memberPicture.getId());
                softly.assertThat(foundMember.getPreferredExercises()).as("선호 운동 정보가 로드되어야 합니다.").isNotEmpty();
                softly.assertThat(foundMember.getPreferredExercises().iterator().next().getId()).as("선호 운동 ID가 일치해야 합니다.").isEqualTo(preferredExercise.getId());
            });
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 조회 시, 빈 Optional을 반환한다")
        void findMemberProfileByMemberIdAndMemberDeletedAtNull_not_exists() {
            // given
            UUID nonExistentMemberId = UUID.randomUUID();

            // when
            Optional<Member> foundMemberOptional = memberRepository.findMemberProfileByMemberId(nonExistentMemberId);

            // then
            assertThat(foundMemberOptional).as("존재하지 않는 회원이므로 빈 Optional을 반환해야 합니다.").isEmpty();
        }

        @Test
        @DisplayName("삭제된 회원 ID로 조회 시, 빈 Optional을 반환한다")
        void findMemberProfileByMemberIdAndMemberDeletedAtNull_deleted_member() {
            // given
            Member deletedMember = createAndSaveMember("deletedUser", "deleted@example.com", true);
            em.flush();
            em.clear();

            // when
            Optional<Member> foundMemberOptional = memberRepository.findMemberProfileByMemberId(deletedMember.getMemberId());

            // then
            assertThat(foundMemberOptional).as("삭제된 회원이므로 빈 Optional을 반환해야 합니다.").isEmpty();
        }
    }
}