package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.event.MemberSignedUpEvent;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.request.UpdateMemberProfileRequest;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.dto.response.UpdateMemberProfileResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceImplTest {

    @InjectMocks
    private MemberCommandServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // 테스트 상수
    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String TEST_EMAIL = "test@email.com";
    private static final String TEST_NICKNAME = "테스트닉네임";

    // 닉네임 변경 테스트를 위한 별도 상수
    private static final String NEW_NICKNAME = "새로운닉네임";
    private static final String DUPLICATE_NICKNAME = "중복된닉네임";

    /**
     * 테스트를 위한 기본 Member 객체를 생성합니다.
     * 정의된 상수를 사용하여 객체를 생성합니다.
     *
     * @return 기본 설정값을 가진 Member 객체
     */
    private Member createDefaultMember() {
        return Member.builder()
                .memberId(TEST_UUID)
                .memberEmail(TEST_EMAIL) // 제공해주신 이메일 상수 추가
                .memberNickname(TEST_NICKNAME) // 제공해주신 닉네임 상수로 변경
                .memberGender(MemberGender.MALE)
                .memberDesc("기존 자기소개")
                .build();
    }

    @Nested
    @DisplayName("멤버 ID 중복 체크")
    class CheckMemberId {
        @Test
        @DisplayName("중복인 경우 true를 반환한다")
        void duplicated_checkMemberId() {
            // given
            given(memberRepository.existsByMemberId(TEST_UUID)).willReturn(true);

            // when
            boolean result = memberService.checkMemberId(TEST_UUID);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("중복이 아닌 경우 false를 반환한다")
        void not_Duplicated_checkMemberId() {
            // given
            given(memberRepository.existsByMemberId(TEST_UUID)).willReturn(false);

            // when
            boolean result = memberService.checkMemberId(TEST_UUID);

            // then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("이메일 중복 체크")
    class CheckMemberEmail {
        @Test
        @DisplayName("중복인 경우 true를 반환한다")
        void duplicated_checkMemberEmail() {
            //given
            given(memberRepository.existsByMemberEmail(TEST_EMAIL)).willReturn(true);
            //when
            boolean result = memberService.checkMemberEmail(TEST_EMAIL);
            //then
            assertTrue(result);
        }

        @Test
        @DisplayName("중복이 아닌 경우 false를 반환한다")
        void not_Duplicated_checkMemberEmail() {
            //given
            given(memberRepository.existsByMemberEmail(TEST_EMAIL)).willReturn(false);
            //when
            boolean result = memberService.checkMemberEmail(TEST_EMAIL);
            //then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("닉네임 중복 체크")
    class CheckMemberNickname {
        @Test
        @DisplayName("중복인 경우 true를 반환한다")
        void duplicated_checkMemberNickname() {
            given(memberRepository.existsByMemberNickname(TEST_NICKNAME)).willReturn(true);
            boolean result = memberService.checkMemberNickname(TEST_NICKNAME);
            assertTrue(result);
        }

        @Test
        @DisplayName("중복이 아닌 경우 false를 반환한다")
        void not_Duplicated_checkMemberNickname() {
            given(memberRepository.existsByMemberNickname(TEST_NICKNAME)).willReturn(false);
            boolean result = memberService.checkMemberNickname(TEST_NICKNAME);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("회원 가입 테스트")
    class MemberSignUpTest {

        private SignUpRequestDto createSignUpRequest() {
            return SignUpRequestDto.builder()
                    .memberNickname(TEST_NICKNAME)
                    .memberGender(MemberGender.MALE)
                    .memberBday(LocalDate.parse("2010-01-01"))
                    .build();
        }

        @Test
        @DisplayName("성공")
        void success_memberSignUp() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
                ignored.when(UserContextHolder::getUserEmail).thenReturn(TEST_EMAIL);

                // given
                SignUpRequestDto requestDto = createSignUpRequest();

                given(memberRepository.existsByMemberId(TEST_UUID)).willReturn(false);
                given(memberRepository.existsByMemberEmail(TEST_EMAIL)).willReturn(false);
                given(memberRepository.existsByMemberNickname(TEST_NICKNAME)).willReturn(false);
                given(policyService.getPolicyValueAsByte(PolicyKey.SIGNUP_INITIAL_POINTS)).willReturn((byte) 35);

                Member member = Member.builder()
                        .memberId(TEST_UUID)
                        .memberEmail(TEST_EMAIL)
                        .memberNickname(TEST_NICKNAME)
                        .memberGender(MemberGender.MALE)
                        .memberScore((byte) 35)
                        .memberBday(LocalDate.parse("2010-01-01"))
                        .build();

                given(memberRepository.save(ArgumentMatchers.any(Member.class))).willReturn(member);

                //when
                SignUpResponseDto result = memberService.memberSignUp(requestDto);

                //then
                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getMemberId()).isEqualTo(TEST_UUID);
                    softly.assertThat(result.getMemberEmail()).isEqualTo(TEST_EMAIL);
                    softly.assertThat(result.getMemberNickname()).isEqualTo(TEST_NICKNAME);
                    softly.assertThat(result.getMemberGender()).isEqualTo('M');
                    softly.assertThat(result.getMemberScore()).isEqualTo((byte) 35);
                    softly.assertThat(result.getMemberBday()).isEqualTo(LocalDate.parse("2010-01-01"));
                    softly.assertThat(result.getMemberCreatedAt()).isNotNull();
                });

                Mockito.verify(memberRepository, times(1)).save(ArgumentMatchers.any(Member.class));

                ArgumentCaptor<MemberSignedUpEvent> eventCaptor = ArgumentCaptor.forClass(MemberSignedUpEvent.class);
                Mockito.verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
                assertEquals(TEST_UUID, eventCaptor.getValue().memberId());
            }
        }

        @Test
        @DisplayName("실패 - 이미 존재하는 멤버 ID")
        void fail_duplicateMemberId() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
                ignored.when(UserContextHolder::getUserEmail).thenReturn(TEST_EMAIL);

                // given
                SignUpRequestDto requestDto = createSignUpRequest();
                given(memberRepository.existsByMemberId(TEST_UUID)).willReturn(true);

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
                assertEquals(ErrorCode.MEMBER_ID_DUPLICATED, exception.getErrorCode());
            }
        }

        @Test
        @DisplayName("실패 - 이미 존재하는 이메일")
        void fail_duplicateEmail() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
                ignored.when(UserContextHolder::getUserEmail).thenReturn(TEST_EMAIL);

                // given
                SignUpRequestDto requestDto = createSignUpRequest();
                given(memberRepository.existsByMemberEmail(TEST_EMAIL)).willReturn(true);

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
                assertEquals(ErrorCode.MEMBER_EMAIL_DUPLICATED, exception.getErrorCode());
            }
        }

        @Test
        @DisplayName("실패 - 이미 존재하는 닉네임")
        void fail_duplicateNickname() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
                ignored.when(UserContextHolder::getUserEmail).thenReturn(TEST_EMAIL);

                // given
                SignUpRequestDto requestDto = createSignUpRequest();
                given(memberRepository.existsByMemberNickname(TEST_NICKNAME)).willReturn(true);

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
                assertEquals(ErrorCode.MEMBER_NICKNAME_DUPLICATED, exception.getErrorCode());
            }
        }

        @Test
        @DisplayName("실패 - 미래 날짜의 생일")
        void fail_invalidBirthday() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
                ignored.when(UserContextHolder::getUserEmail).thenReturn(TEST_EMAIL);

                // given
                SignUpRequestDto requestDto = SignUpRequestDto.builder()
                        .memberNickname(TEST_NICKNAME)
                        .memberGender(MemberGender.MALE)
                        .memberBday(LocalDate.now().plusDays(1))
                        .build();

                given(memberRepository.existsByMemberId(TEST_UUID)).willReturn(false);
                given(memberRepository.existsByMemberEmail(TEST_EMAIL)).willReturn(false);
                given(memberRepository.existsByMemberNickname(TEST_NICKNAME)).willReturn(false);

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
                assertEquals(ErrorCode.MEMBER_BDAY_ERROR, exception.getErrorCode());
            }
        }
    }

    // --- Helper Methods ---

    @Nested
    @DisplayName("회원 프로필 수정 테스트")
    class UpdateMemberProfileTest {

        @Test
        @DisplayName("성공 - 닉네임을 중복되지 않는 새 닉네임으로 변경")
        void success_whenNicknameIsChangedAndNotDuplicate() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // given
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);

                Member member = createDefaultMember();
                UpdateMemberProfileRequest request = new UpdateMemberProfileRequest(NEW_NICKNAME, MemberGender.FEMALE, "새로운 자기소개");

                given(memberRepository.findById(TEST_UUID)).willReturn(Optional.of(member));
                given(memberRepository.existsByMemberNickname(NEW_NICKNAME)).willReturn(false);

                // when
                UpdateMemberProfileResponse response = memberService.updateMemberProfile(request);

                // then
                assertSoftly(softly -> {
                    softly.assertThat(response.getNickname()).isEqualTo(NEW_NICKNAME);
                    softly.assertThat(response.getGender()).isEqualTo(MemberGender.FEMALE.toString());
                });

                assertThat(member.getMemberNickname()).isEqualTo(NEW_NICKNAME);

                then(memberRepository).should(times(1)).findById(TEST_UUID);
                then(memberRepository).should(times(1)).existsByMemberNickname(NEW_NICKNAME);
            }
        }

        @Test
        @DisplayName("성공 - 닉네임 변경 없이 다른 정보만 수정")
        void success_whenNicknameIsNotChanged() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // given
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);

                Member member = createDefaultMember();
                // 요청 DTO의 닉네임을 기존 닉네임 상수(TEST_NICKNAME)로 설정
                UpdateMemberProfileRequest request = new UpdateMemberProfileRequest(TEST_NICKNAME, MemberGender.FEMALE, "자기소개만 변경");

                given(memberRepository.findById(TEST_UUID)).willReturn(Optional.of(member));

                // when
                memberService.updateMemberProfile(request);

                // then
                then(memberRepository).should(never()).existsByMemberNickname(anyString());

                assertThat(member.getMemberNickname()).isEqualTo(TEST_NICKNAME);
                assertThat(member.getMemberDesc()).isEqualTo("자기소개만 변경");
            }
        }

        @Test
        @DisplayName("실패 - 변경하려는 닉네임이 다른 사용자에 의해 사용 중")
        void fail_whenNicknameIsDuplicated() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // given
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);

                Member member = createDefaultMember();
                UpdateMemberProfileRequest request = new UpdateMemberProfileRequest(DUPLICATE_NICKNAME, MemberGender.FEMALE, "자기소개 변경");

                given(memberRepository.findById(TEST_UUID)).willReturn(Optional.of(member));
                given(memberRepository.existsByMemberNickname(DUPLICATE_NICKNAME)).willReturn(true);

                // when & then
                CustomException exception = assertThrows(CustomException.class,
                        () -> memberService.updateMemberProfile(request));

                assertEquals(ErrorCode.MEMBER_NICKNAME_DUPLICATED, exception.getErrorCode());
                assertThat(member.getMemberNickname()).isEqualTo(TEST_NICKNAME);
            }
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원")
        void fail_whenMemberNotFound() {
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // given
                ignored.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
                UpdateMemberProfileRequest request = new UpdateMemberProfileRequest(NEW_NICKNAME, MemberGender.FEMALE, "새로운 자기소개");

                given(memberRepository.findById(TEST_UUID)).willReturn(Optional.empty());

                // when & then
                CustomException exception = assertThrows(CustomException.class,
                        () -> memberService.updateMemberProfile(request));

                assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
                then(memberRepository).should(never()).existsByMemberNickname(anyString());
            }
        }
    }
}