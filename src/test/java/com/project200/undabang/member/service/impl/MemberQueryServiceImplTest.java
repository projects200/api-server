package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.dto.response.*;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceImplTest {

    @InjectMocks
    private MemberQueryServiceImpl memberQueryService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PolicyService policyService;

    /**
     * 등록된 회원의 상태를 확인하는 테스트
     */
    @Test
    @DisplayName("회원이 등록되어 있을 경우 회원 상태는 등록됨으로 반환된다")
    void getRegistrationStatus_RegisteredMember_ReturnsTrue() {
        UUID testUserId = UUID.randomUUID();

        try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            // given
            given(UserContextHolder.getUserId()).willReturn(testUserId);
            given(memberRepository.existsByMemberId(testUserId)).willReturn(true);

            // when
            MemberRegistrationStatusResponseDto response = memberQueryService.getRegistrationStatus();

            // then
            assertThat(response.getMemberId()).isEqualTo(testUserId);
            assertThat(response.isRegistered()).isTrue();
        }
    }

    /**
     * 등록되지 않은 회원의 상태를 확인하는 테스트
     */
    @Test
    @DisplayName("회원이 등록되어 있지 않을 경우 회원 상태는 미등록으로 반환된다")
    void getRegistrationStatus_UnregisteredMember_ReturnsFalse() {
        UUID testUserId = UUID.randomUUID();

        try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            // given
            given(UserContextHolder.getUserId()).willReturn(testUserId);
            given(memberRepository.existsByMemberId(testUserId)).willReturn(false);

            // when
            MemberRegistrationStatusResponseDto response = memberQueryService.getRegistrationStatus();

            // then
            assertThat(response.getMemberId()).isEqualTo(testUserId);
            assertThat(response.isRegistered()).isFalse();
        }
    }

    private void setPreferredExercise(Member member) {
        ExerciseType exerciseType = ExerciseType.builder()
                .id(1L)
                .exerciseName("헬스")
                .exerciseTypeImageUrl("http://example.com/exercise/weight_training.jpg")
                .build();

        // 테스트용 선호 운동 설정
        PreferredExercise preferredExercise = PreferredExercise.builder()
                .id(1L)
                .exercise(exerciseType)
                .member(member)
                .preferredExerciseSkillLevel(ExerciseSkillLevel.PRO)
                .build();

        preferredExercise.setDaysOfWeek(new boolean[]{false, false, true, true, true, true, true});

        ReflectionTestUtils.setField(member, "preferredExercises", List.of(preferredExercise));
    }

    private void setMemberPicture(Member member) {
        Picture picture = Picture.builder()
                .id(1L)
                .pictureName("profile_image.jpg")
                .pictureExtension(".jpg")
                .pictureSize(1024)
                .pictureUrl("http://example.com/profile_image.jpg")
                .build();

        MemberPicture memberPicture = MemberPicture.builder()
                .id(1L)
                .memberPicturesUrl("http://example.com/profile_thumbnail.jpg")
                .picture(picture)
                .member(member)
                .memberPicturesName("profile_thumbnail.jpg")
                .memberPicturesUrl("http://example.com/profile_thumbnail.jpg")
                .build();

        member.updateProfilePicture(memberPicture);
    }

    private Member createMember() {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("test@gmail.com")
                .memberNickname("테스트유저")
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(1990, 1, 1))
                .memberScore((byte) 50)
                .memberDesc("테스트 자기소개입니다.")
                .build();
    }

    @Nested
    @DisplayName("운동기록 조회")
    class findMemberExerciseScore {
        @Test
        @DisplayName("회원의 운동점수 기록 조회")
        void findMemberExerciseScore() {
            Member testMember = createMember();
            UUID testMemberId = testMember.getMemberId();

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                given(UserContextHolder.getUserId()).willReturn(testMemberId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testMemberId)).willReturn(Optional.of(testMember));
                given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_SCORE_MAX_POINTS)).willReturn(100);
                given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_SCORE_MIN_POINTS)).willReturn(0);

                MemberScoreResponseDto respDto = memberQueryService.getMemberScore();


                Assertions.assertThat(respDto).isNotNull();
                Assertions.assertThat(respDto.getMemberScore()).isEqualTo(testMember.getMemberScore());
                Assertions.assertThat(respDto.getMemberId()).isEqualTo(testMemberId);
                Assertions.assertThat(respDto.getPolicyMaxScore()).isEqualTo(100);
                Assertions.assertThat(respDto.getPolicyMinScore()).isEqualTo(0);

                then(memberRepository).should(times(1)).findByMemberIdAndMemberDeletedAtNull(testMemberId);
            }
        }

        @Test
        @DisplayName("회원의 운동점수 기록 조회_운동기록 없음")
        void findMemberExerciseScore_NotHavingExerciseRecord() {
            UUID testMemberId = UUID.randomUUID();
            Byte initialScore = 35; // 초기 점수 설정
            Member testMember = Member.builder().memberId(testMemberId).memberScore(initialScore).build();

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                given(UserContextHolder.getUserId()).willReturn(testMemberId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testMemberId)).willReturn(Optional.of(testMember));
                given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_SCORE_MAX_POINTS)).willReturn(100);
                given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_SCORE_MIN_POINTS)).willReturn(0);

                MemberScoreResponseDto respDto = memberQueryService.getMemberScore();

                Assertions.assertThat(respDto).isNotNull();
                Assertions.assertThat(respDto.getMemberScore()).isEqualTo((byte) 35);
                Assertions.assertThat(respDto.getMemberId()).isEqualTo(testMemberId);
                Assertions.assertThat(respDto.getPolicyMaxScore()).isEqualTo(100);
                Assertions.assertThat(respDto.getPolicyMinScore()).isEqualTo(0);

                then(memberRepository).should(times(1)).findByMemberIdAndMemberDeletedAtNull(testMemberId);
            }
        }

        @Test
        @DisplayName("회원의 운동점수 기록 조회_실패하는 경우")
        void findMemberExerciseScore_Fail() {
            UUID testMemberId = UUID.randomUUID();
            given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testMemberId)).willReturn(Optional.empty());
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                given(UserContextHolder.getUserId()).willReturn(testMemberId);

                assertThatThrownBy(() -> memberQueryService.getMemberScore())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                then(memberRepository).should(times(1)).findByMemberIdAndMemberDeletedAtNull(testMemberId);
            }
        }

        private Member createMember() {
            return Member.builder()
                    .memberId(UUID.randomUUID())
                    .memberScore((byte) 50)
                    .build();
        }
    }

    @Nested
    @DisplayName("getMemberProfile 메소드는")
    class GetMemberProfile {

        @Test
        @DisplayName("성공적으로 모든 정보가 담긴 회원 프로필 정보를 반환한다")
        void getMemberProfileAllData_Success() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member mockMember = createMember();
            setMemberPicture(mockMember);
            setPreferredExercise(mockMember);

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(testUserId))
                        .willReturn(Optional.of(mockMember));

                // when
                MemberProfileResponse response = memberQueryService.getMemberProfile();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(response).isNotNull();
                    softly.assertThat(response.getProfileThumbnailUrl()).isEqualTo("http://example.com/profile_thumbnail.jpg");
                    softly.assertThat(response.getProfileImageUrl()).isEqualTo("http://example.com/profile_image.jpg");
                    softly.assertThat(response.getNickname()).isEqualTo("테스트유저");
                    softly.assertThat(response.getGender()).isEqualTo(MemberGender.UNKNOWN);
                    softly.assertThat(response.getBirthDate()).isEqualTo("1990-01-01");
                    softly.assertThat(response.getBio()).isEqualTo("테스트 자기소개입니다.");
                    softly.assertThat(response.getYearlyExerciseDays()).isEqualTo(0);
                    softly.assertThat(response.getExerciseCountInLast30Days()).isEqualTo(0);
                    softly.assertThat(response.getExerciseScore()).isEqualTo(50);
                    softly.assertThat(response.getPreferredExercises()).isNotNull();
                    softly.assertThat(response.getPreferredExercises()).hasSize(1);
                    softly.assertThat(response.getPreferredExercises().getFirst().getPreferredExerciseId()).isEqualTo(1L);
                    softly.assertThat(response.getPreferredExercises().getFirst().getName()).isEqualTo("헬스");
                    softly.assertThat(response.getPreferredExercises().getFirst().getImageUrl()).isEqualTo("http://example.com/exercise/weight_training.jpg");
                    softly.assertThat(response.getPreferredExercises().getFirst().getSkillLevel()).isEqualTo(ExerciseSkillLevel.PRO);
                    softly.assertThat(response.getPreferredExercises().getFirst().getDaysOfWeek()).isNotNull();
                    softly.assertThat(response.getPreferredExercises().getFirst().getDaysOfWeek()).isEqualTo(new boolean[]{false, false, true, true, true, true, true});
                });
            }
        }

        @Test
        @DisplayName("오직 회원 관련 정보만 있는 경우 성공적으로 회원 프로필 정보를 반환한다")
        void getMemberProfileOnlyMember_Success() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member mockMember = createMember();

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(testUserId))
                        .willReturn(Optional.of(mockMember));

                // when
                MemberProfileResponse response = memberQueryService.getMemberProfile();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(response).isNotNull();
                    softly.assertThat(response.getProfileThumbnailUrl()).isNull();
                    softly.assertThat(response.getProfileImageUrl()).isNull();
                    softly.assertThat(response.getNickname()).isEqualTo("테스트유저");
                    softly.assertThat(response.getGender()).isEqualTo(MemberGender.UNKNOWN);
                    softly.assertThat(response.getBirthDate()).isEqualTo("1990-01-01");
                    softly.assertThat(response.getBio()).isEqualTo("테스트 자기소개입니다.");
                    softly.assertThat(response.getYearlyExerciseDays()).isEqualTo(0);
                    softly.assertThat(response.getExerciseCountInLast30Days()).isEqualTo(0);
                    softly.assertThat(response.getExerciseScore()).isEqualTo(50);
                    softly.assertThat(response.getPreferredExercises()).isEmpty();
                });
            }
        }

        @Test
        @DisplayName("회원을 찾을 수 없을 때 MEMBER_NOT_FOUND 예외를 던진다")
        void getMemberProfile_ThrowsMemberNotFound() {
            // given
            UUID testUserId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> mockedStatic = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(testUserId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberQueryService.getMemberProfile())
                        .as("존재하지 않는 회원 조회 시 예외가 발생해야 합니다.")
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("운동 기록 횟수가 정확히 포함된 프로필 정보를 반환한다")
        void getMemberProfile_withExerciseCounts_Success() {
            // given: 테스트에 필요한 mock 데이터 설정
            UUID testUserId = UUID.randomUUID();
            Member mockMember = createMember();
            // Mocking할 때 일관성을 유지하기 위해 Member의 ID를 testUserId로 설정합니다.
            ReflectionTestUtils.setField(mockMember, "memberId", testUserId);

            // Repository 메서드들이 반환할 값들을 미리 정의합니다.
            long expectedYearlyCount = 15L;
            long expectedMonthlyCount = 5L;

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // 1. UserContextHolder가 testUserId를 반환하도록 설정
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);

                // 2. 기본 프로필 조회 시 mockMember를 반환하도록 설정
                BDDMockito.given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(testUserId))
                        .willReturn(Optional.of(mockMember));

                // 3. 올해 운동 일수 조회 시 expectedYearlyCount를 반환하도록 설정
                BDDMockito.given(memberRepository.countMemberExerciseInThisYear(testUserId))
                        .willReturn(expectedYearlyCount);

                // 4. 최근 30일 운동 횟수 조회 시 expectedMonthlyCount를 반환하도록 설정
                BDDMockito.given(memberRepository.countMemberExerciseInLastDays(testUserId, 30))
                        .willReturn(expectedMonthlyCount);

                // when: 실제 서비스 메서드 호출
                MemberProfileResponse response = memberQueryService.getMemberProfile();

                // then: 반환된 DTO의 값이 우리가 Mocking한 값과 일치하는지 검증
                assertSoftly(softly -> {
                    softly.assertThat(response).isNotNull();
                    softly.assertThat(response.getNickname()).isEqualTo("테스트유저"); // 기본 정보 확인
                    softly.assertThat(response.getExerciseScore()).isEqualTo(50); // 기본 정보 확인

                    // 핵심 검증: 운동 기록 횟수가 정확히 매핑되었는지 확인
                    softly.assertThat(response.getYearlyExerciseDays()).isEqualTo((int) expectedYearlyCount);
                    softly.assertThat(response.getExerciseCountInLast30Days()).isEqualTo((int) expectedMonthlyCount);
                });

                // then: Repository의 메서드들이 정확히 1번씩 호출되었는지 검증 (Verify)
                then(memberRepository).should(times(1)).findMemberProfileByMemberIdAndMemberDeletedAtNull(testUserId);
                then(memberRepository).should(times(1)).countMemberExerciseInThisYear(testUserId);
                then(memberRepository).should(times(1)).countMemberExerciseInLastDays(testUserId, 30);
            }
        }
    }


    @Nested
    @DisplayName("닉네임 중복 검사")
    class CheckDuplicateNickname {

        @Test
        @DisplayName("이미 존재하는 닉네임일 경우 available false를 반환한다")
        void checkDuplicateNickname_ExistingNickname_ReturnsFalse() {
            // given
            String existingNickname = "이미존재하는닉네임";
            given(memberRepository.existsByMemberNickname(existingNickname)).willReturn(true);

            // when
            CheckNicknameDuplicateResponse response = memberQueryService.checkDuplicateNickname(existingNickname);

            // then
            assertThat(response.isAvailable()).isFalse();
            then(memberRepository).should().existsByMemberNickname(existingNickname);
        }

        @Test
        @DisplayName("사용 가능한 닉네임일 경우 available true를 반환한다")
        void checkDuplicateNickname_AvailableNickname_ReturnsTrue() {
            // given
            String availableNickname = "새로운닉네임";
            given(memberRepository.existsByMemberNickname(availableNickname)).willReturn(false);

            // when
            CheckNicknameDuplicateResponse response = memberQueryService.checkDuplicateNickname(availableNickname);

            // then
            assertThat(response.isAvailable()).isTrue();
            then(memberRepository).should().existsByMemberNickname(availableNickname);
        }
    }

    @Nested
    @DisplayName("다른 회원 프로필 조회")
    class GetOtherMemberProfile {

        @Test
        @DisplayName("성공적으로 다른 회원의 프로필 정보를 반환한다")
        void getOtherMemberProfile_Success() {
            // given
            UUID otherMemberId = UUID.randomUUID();
            Member mockMember = createMember();
            ReflectionTestUtils.setField(mockMember, "memberId", otherMemberId);
            setMemberPicture(mockMember);
            setPreferredExercise(mockMember);

            long expectedYearlyCount = 15L;
            long expectedMonthlyCount = 5L;

            BDDMockito.given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherMemberId))
                    .willReturn(Optional.of(mockMember));
            BDDMockito.given(memberRepository.countMemberExerciseInThisYear(otherMemberId))
                    .willReturn(expectedYearlyCount);
            BDDMockito.given(memberRepository.countMemberExerciseInLastDays(otherMemberId, 30))
                    .willReturn(expectedMonthlyCount);

            // when
            GetOtherMemberProfileResponse response = memberQueryService.getOtherMemberProfile(otherMemberId);

            // then
            assertSoftly(softly -> {
                softly.assertThat(response).isNotNull();
                softly.assertThat(response.getProfileThumbnailUrl()).isEqualTo("http://example.com/profile_thumbnail.jpg");
                softly.assertThat(response.getProfileImageUrl()).isEqualTo("http://example.com/profile_image.jpg");
                softly.assertThat(response.getNickname()).isEqualTo("테스트유저");
                softly.assertThat(response.getGender()).isEqualTo(MemberGender.UNKNOWN);
                softly.assertThat(response.getBirthDate()).isEqualTo("1990-01-01");
                softly.assertThat(response.getBio()).isEqualTo("테스트 자기소개입니다.");
                softly.assertThat(response.getYearlyExerciseDays()).isEqualTo((int) expectedYearlyCount);
                softly.assertThat(response.getExerciseCountInLast30Days()).isEqualTo((int) expectedMonthlyCount);
                softly.assertThat(response.getExerciseScore()).isEqualTo(50);
                softly.assertThat(response.getPreferredExercises()).hasSize(1);
            });

            then(memberRepository).should(times(1)).findMemberProfileByMemberIdAndMemberDeletedAtNull(otherMemberId);
            then(memberRepository).should(times(1)).countMemberExerciseInThisYear(otherMemberId);
            then(memberRepository).should(times(1)).countMemberExerciseInLastDays(otherMemberId, 30);
        }

        @Test
        @DisplayName("회원 정보만 있는 경우 성공적으로 다른 회원 프로필 정보를 반환한다")
        void getOtherMemberProfileOnlyMember_Success() {
            // given
            UUID otherMemberId = UUID.randomUUID();
            Member mockMember = createMember();
            ReflectionTestUtils.setField(mockMember, "memberId", otherMemberId);

            long expectedYearlyCount = 0L;
            long expectedMonthlyCount = 0L;

            BDDMockito.given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherMemberId))
                    .willReturn(Optional.of(mockMember));
            BDDMockito.given(memberRepository.countMemberExerciseInThisYear(otherMemberId))
                    .willReturn(expectedYearlyCount);
            BDDMockito.given(memberRepository.countMemberExerciseInLastDays(otherMemberId, 30))
                    .willReturn(expectedMonthlyCount);

            // when
            GetOtherMemberProfileResponse response = memberQueryService.getOtherMemberProfile(otherMemberId);

            // then
            assertSoftly(softly -> {
                softly.assertThat(response).isNotNull();
                softly.assertThat(response.getProfileThumbnailUrl()).isNull();
                softly.assertThat(response.getProfileImageUrl()).isNull();
                softly.assertThat(response.getNickname()).isEqualTo("테스트유저");
                softly.assertThat(response.getYearlyExerciseDays()).isEqualTo(0);
                softly.assertThat(response.getExerciseCountInLast30Days()).isEqualTo(0);
                softly.assertThat(response.getPreferredExercises()).isEmpty();
            });
        }

        @Test
        @DisplayName("회원을 찾을 수 없을 때 MEMBER_NOT_FOUND 예외를 던진다")
        void getOtherMemberProfile_ThrowsMemberNotFound() {
            // given
            UUID otherMemberId = UUID.randomUUID();
            BDDMockito.given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherMemberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberQueryService.getOtherMemberProfile(otherMemberId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

            then(memberRepository).should(times(1)).findMemberProfileByMemberIdAndMemberDeletedAtNull(otherMemberId);
        }

        @Test
        @DisplayName("본인 UUID로 다른 회원 프로필 조회 시 MEMBER_SELF_REQUEST_NOT_ALLOWED 예외를 던진다")
        void getOtherMemberProfile_SelfRequest_ThrowsException() {
            UUID selfId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(selfId);

                assertThatThrownBy(() -> memberQueryService.getOtherMemberProfile(selfId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
            }
        }

    }
}