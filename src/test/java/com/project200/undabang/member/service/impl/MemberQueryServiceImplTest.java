package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.dto.response.*;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceImplTest {

    @InjectMocks
    private MemberQueryServiceImpl memberQueryService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private PolicyService policyService;

    private Member createMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberEmail("test@gmail.com")
                .memberNickname("테스트유저")
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(1990, 1, 1))
                .memberScore((byte) 50)
                .memberDesc("테스트 자기소개입니다.")
                .build();
    }

    private Member createMemberWithFullProfile(UUID memberId) {
        Member member = createMember(memberId);
        addPictureToMember(member);
        addPreferredExerciseToMember(member, "헬스", false);
        return member;
    }

    private Member createMemberWithMixedPreferredExercises(UUID memberId) {
        Member member = createMember(memberId);
        addPreferredExerciseToMember(member, "헬스", false); // 활성
        addPreferredExerciseToMember(member, "요가", true);  // 삭제됨
        return member;
    }

    private void addPictureToMember(Member member) {
        Picture picture = Picture.builder()
                .id(1L)
                .pictureName("profile_image.jpg")
                .pictureUrl("http://example.com/profile_image.jpg")
                .build();

        MemberPicture memberPicture = MemberPicture.builder()
                .id(1L)
                .picture(picture)
                .member(member)
                .memberPicturesUrl("http://example.com/profile_image.jpg")
                .build();

        member.updateProfilePicture(memberPicture);
    }

    private void addPreferredExerciseToMember(Member member, String exerciseName, boolean isDeleted) {
        ExerciseType exerciseType = ExerciseType.builder()
                .id(1L)
                .exerciseName(exerciseName)
                .exerciseTypeImageUrl("http://example.com/exercise/weight_training.jpg")
                .build();

        boolean[] days = {false, false, true, true, true, true, true};
        PreferredExercise preferredExercise = PreferredExercise.createPreferredExercise(
                member,
                exerciseType,
                ExerciseSkillLevel.PRO,
                days
        );

        if (isDeleted) {
            preferredExercise.delete();
        }

        List<PreferredExercise> currentList = (List<PreferredExercise>) ReflectionTestUtils.getField(member, "preferredExercises");
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        List<PreferredExercise> newList = new ArrayList<>(currentList);
        newList.add(preferredExercise);

        ReflectionTestUtils.setField(member, "preferredExercises", newList);
    }

    @Nested
    @DisplayName("getRegistrationStatus 메소드는")
    class GetRegistrationStatus {

        @Test
        @DisplayName("이미 가입된 회원이면 registered=true를 반환한다")
        void returnsTrue_WhenMemberExists() {
            UUID userId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.existsByMemberId(userId)).willReturn(true);

                MemberRegistrationStatusResponseDto response = memberQueryService.getRegistrationStatus();

                assertSoftly(softly -> {
                    softly.assertThat(response.getMemberId()).isEqualTo(userId);
                    softly.assertThat(response.isRegistered()).isTrue();
                });
            }
        }

        @Test
        @DisplayName("가입되지 않은 회원이면 registered=false를 반환한다")
        void returnsFalse_WhenMemberDoesNotExist() {
            UUID userId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.existsByMemberId(userId)).willReturn(false);

                MemberRegistrationStatusResponseDto response = memberQueryService.getRegistrationStatus();

                assertSoftly(softly -> {
                    softly.assertThat(response.getMemberId()).isEqualTo(userId);
                    softly.assertThat(response.isRegistered()).isFalse();
                });
            }
        }
    }

    @Nested
    @DisplayName("getMemberScore 메소드는")
    class GetMemberScore {

        @Test
        @DisplayName("회원의 현재 점수와 정책상 최대/최소 점수를 반환한다")
        void returnsScoreAndPolicyLimits() {
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_SCORE_MAX_POINTS)).willReturn(100);
                given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_SCORE_MIN_POINTS)).willReturn(0);

                MemberScoreResponseDto response = memberQueryService.getMemberScore();

                assertSoftly(softly -> {
                    softly.assertThat(response.getMemberId()).isEqualTo(userId);
                    softly.assertThat(response.getMemberScore()).isEqualTo(member.getMemberScore());
                    softly.assertThat(response.getPolicyMaxScore()).isEqualTo(100);
                    softly.assertThat(response.getPolicyMinScore()).isEqualTo(0);
                });
            }
        }

        @Test
        @DisplayName("회원을 찾을 수 없으면 MEMBER_NOT_FOUND 예외를 던진다")
        void throwsException_WhenMemberNotFound() {
            UUID userId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> memberQueryService.getMemberScore())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getMemberProfile 메소드는")
    class GetMemberProfile {

        @Test
        @DisplayName("프로필 사진과 선호 운동이 있는 회원의 전체 프로필 정보를 반환한다")
        void returnsFullProfile_WhenAllDataExists() {
            UUID userId = UUID.randomUUID();
            Member member = createMemberWithFullProfile(userId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(memberRepository.countMemberExerciseInThisYear(userId)).willReturn(10L);
                given(memberRepository.countMemberExerciseInLastDays(userId, 30)).willReturn(5L);

                MemberProfileResponse response = memberQueryService.getMemberProfile();

                assertSoftly(softly -> {
                    softly.assertThat(response.getNickname()).isEqualTo("테스트유저");
                    softly.assertThat(response.getProfileImageUrl()).isEqualTo("http://example.com/profile_image.jpg");
                    softly.assertThat(response.getPreferredExercises()).hasSize(1);
                    softly.assertThat(response.getPreferredExercises().getFirst().getName()).isEqualTo("헬스");
                    softly.assertThat(response.getYearlyExerciseDays()).isEqualTo(10);
                    softly.assertThat(response.getExerciseCountInLast30Days()).isEqualTo(5);
                });
            }
        }

        @Test
        @DisplayName("삭제된 선호 운동은 제외하고 반환한다")
        void filtersOutDeletedPreferredExercises() {
            UUID userId = UUID.randomUUID();
            Member member = createMemberWithMixedPreferredExercises(userId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));

                MemberProfileResponse response = memberQueryService.getMemberProfile();

                assertThat(response.getPreferredExercises()).hasSize(1);
                assertThat(response.getPreferredExercises().getFirst().getName()).isEqualTo("헬스");
            }
        }

        @Test
        @DisplayName("회원 정보가 없으면 예외를 던진다")
        void throwsException_WhenMemberNotFound() {
            UUID userId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> memberQueryService.getMemberProfile())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("checkDuplicateNickname 메소드는")
    class CheckDuplicateNickname {

        @Test
        @DisplayName("중복된 닉네임이면 available=false를 반환한다")
        void returnsFalse_WhenNicknameExists() {
            String nickname = "중복닉네임";
            given(memberRepository.existsByMemberNickname(nickname)).willReturn(true);

            CheckNicknameDuplicateResponse response = memberQueryService.checkDuplicateNickname(nickname);

            assertThat(response.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("사용 가능한 닉네임이면 available=true를 반환한다")
        void returnsTrue_WhenNicknameIsNew() {
            String nickname = "새닉네임";
            given(memberRepository.existsByMemberNickname(nickname)).willReturn(false);

            CheckNicknameDuplicateResponse response = memberQueryService.checkDuplicateNickname(nickname);

            assertThat(response.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("getOtherMemberCalendars 메소드는")
    class GetOtherMemberCalendars {

        @Test
        @DisplayName("다른 회원의 운동 기록 목록을 정상 반환한다")
        void returnsCalendarData_Success() {
            UUID myId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            LocalDate start = LocalDate.now().minusDays(5);
            LocalDate end = LocalDate.now();
            Member otherMember = createMember(otherId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherId)).willReturn(Optional.of(otherMember));
                given(exerciseRepository.findExercisesByPeriod(otherId, start, end))
                        .willReturn(List.of(new FindExerciseRecordByPeriodResponseDto(end, 1L)));

                List<FindExerciseRecordByPeriodResponseDto> result = memberQueryService.getOtherMemberCalendars(otherId, start, end);

                assertThat(result).hasSize(1);
            }
        }

        @Test
        @DisplayName("본인 ID로 요청 시 예외를 던진다 (Self Request)")
        void throwsException_WhenRequestingSelf() {
            UUID myId = UUID.randomUUID();
            LocalDate now = LocalDate.now();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);

                assertThatThrownBy(() -> memberQueryService.getOtherMemberCalendars(myId, now, now))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
            }
        }

        @Test
        @DisplayName("시작 날짜가 종료 날짜보다 늦으면 예외를 던진다")
        void throwsException_WhenStartAfterEnd() {
            UUID myId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            LocalDate start = LocalDate.now();
            LocalDate end = LocalDate.now().minusDays(1);
            Member otherMember = createMember(otherId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherId)).willReturn(Optional.of(otherMember));

                assertThatThrownBy(() -> memberQueryService.getOtherMemberCalendars(otherId, start, end))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMPOSSIBLE_INPUT_DATE);
            }
        }

        @Test
        @DisplayName("유효하지 않은 날짜 범위(미래 혹은 너무 먼 과거)면 예외를 던진다")
        void throwsException_WhenDateInvalid() {
            UUID myId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Member otherMember = createMember(otherId);
            LocalDate validStart = LocalDate.now().minusDays(1);
            LocalDate futureEnd = LocalDate.now().plusDays(1);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherId)).willReturn(Optional.of(otherMember));

                assertThatThrownBy(() -> memberQueryService.getOtherMemberCalendars(otherId, validStart, futureEnd))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }

    @Nested
    @DisplayName("getOtherMemberProfile 메소드는")
    class GetOtherMemberProfile {

        @Test
        @DisplayName("다른 회원의 프로필 정보를 정상 반환한다 (사진 및 선호운동 포함)")
        void returnsOtherProfile_Success() {
            // given
            UUID myId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Member otherMember = createMemberWithFullProfile(otherId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherId))
                        .willReturn(Optional.of(otherMember));
                // 운동 통계 Mocking
                given(memberRepository.countMemberExerciseInThisYear(otherId)).willReturn(20L);
                given(memberRepository.countMemberExerciseInLastDays(otherId, 30)).willReturn(3L);

                // when
                GetOtherMemberProfileResponse response = memberQueryService.getOtherMemberProfile(otherId);

                // then
                assertSoftly(softly -> {
                    softly.assertThat(response.getNickname()).isEqualTo("테스트유저");
                    softly.assertThat(response.getProfileImageUrl()).isEqualTo("http://example.com/profile_image.jpg"); // 사진 URL 확인
                    softly.assertThat(response.getYearlyExerciseDays()).isEqualTo(20);
                    softly.assertThat(response.getExerciseCountInLast30Days()).isEqualTo(3);
                    softly.assertThat(response.getPreferredExercises()).hasSize(1); // 선호운동 개수 확인
                });
            }
        }

        @Test
        @DisplayName("프로필 사진이 없는 회원의 경우 URL 필드에 null을 반환한다")
        void returnsNullUrl_WhenNoPicture() {
            // given
            UUID myId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Member otherMember = createMember(otherId); // 사진 없이 생성

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherId))
                        .willReturn(Optional.of(otherMember));
                given(memberRepository.countMemberExerciseInThisYear(otherId)).willReturn(0L);
                given(memberRepository.countMemberExerciseInLastDays(otherId, 30)).willReturn(0L);

                // when
                GetOtherMemberProfileResponse response = memberQueryService.getOtherMemberProfile(otherId);

                // then
                assertSoftly(softly -> {
                    softly.assertThat(response.getProfileImageUrl()).isNull();
                    softly.assertThat(response.getProfileThumbnailUrl()).isNull();
                });
            }
        }

        @Test
        @DisplayName("삭제된 선호 운동은 결과 목록에서 제외한다")
        void filtersOutDeletedPreferredExercises() {
            // given
            UUID myId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            // 활성 운동 1개, 삭제된 운동 1개가 섞인 멤버 생성
            Member otherMember = createMemberWithMixedPreferredExercises(otherId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherId))
                        .willReturn(Optional.of(otherMember));
                given(memberRepository.countMemberExerciseInThisYear(otherId)).willReturn(10L);
                given(memberRepository.countMemberExerciseInLastDays(otherId, 30)).willReturn(5L);

                // when
                GetOtherMemberProfileResponse response = memberQueryService.getOtherMemberProfile(otherId);

                // then
                assertSoftly(softly -> {
                    softly.assertThat(response.getPreferredExercises()).hasSize(1);
                    softly.assertThat(response.getPreferredExercises().getFirst().getName()).isEqualTo("헬스");
                    // "요가"는 삭제되었으므로 포함되지 않아야 함
                });
            }
        }

        @Test
        @DisplayName("본인 ID로 조회 요청 시 예외를 던진다")
        void throwsException_WhenRequestingSelf() {
            UUID myId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);

                assertThatThrownBy(() -> memberQueryService.getOtherMemberProfile(myId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
            }
        }

        @Test
        @DisplayName("존재하지 않는 회원 조회 시 예외를 던진다")
        void throwsException_WhenMemberNotFound() {
            UUID myId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(myId);
                given(memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(otherId))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() -> memberQueryService.getOtherMemberProfile(otherId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }
}