package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceImplTest {

    @InjectMocks
    private MemberQueryServiceImpl memberQueryService;

    @Mock
    private MemberRepository memberRepository;

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

    @Nested
    @DisplayName("운동기록 조회")
    class findMemberExerciseScore{
        @Test
        @DisplayName("회원의 운동점수 기록 조회")
        void findMemberExerciseScore(){
            UUID testMemberId = UUID.randomUUID();
            Byte expectedScore = 50;
            Member testMember = Member.builder().memberId(testMemberId).memberScore(expectedScore).build();

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                given(UserContextHolder.getUserId()).willReturn(testMemberId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testMemberId)).willReturn(Optional.of(testMember));

                MemberScoreResponseDto respDto = memberQueryService.getMemberScore();

                Assertions.assertThat(respDto).isNotNull();
                Assertions.assertThat(respDto.getMemberScore()).isEqualTo(expectedScore);
                Assertions.assertThat(respDto.getMemberId()).isEqualTo(testMemberId);

                then(memberRepository).should(times(1)).findByMemberIdAndMemberDeletedAtNull(testMemberId);
            }
        }

        @Test
        @DisplayName("회원의 운동점수 기록 조회_운동기록 없음")
        void findMemberExerciseScore_NotHavingExerciseRecord(){
            UUID testMemberId = UUID.randomUUID();
            Member testMember = Member.builder().memberId(testMemberId).build();

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                given(UserContextHolder.getUserId()).willReturn(testMemberId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testMemberId)).willReturn(Optional.of(testMember));

                MemberScoreResponseDto respDto = memberQueryService.getMemberScore();

                Assertions.assertThat(respDto).isNotNull();
                Assertions.assertThat(respDto.getMemberScore()).isEqualTo((byte)35);
                Assertions.assertThat(respDto.getMemberId()).isEqualTo(testMemberId);

                then(memberRepository).should(times(1)).findByMemberIdAndMemberDeletedAtNull(testMemberId);
            }
        }

        @Test
        @DisplayName("회원의 운동점수 기록 조회_실패하는 경우")
        void findMemberExerciseScore_Fail(){
            UUID testMemberId = UUID.randomUUID();
            given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testMemberId)).willReturn(Optional.empty());
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                given(UserContextHolder.getUserId()).willReturn(testMemberId);

                Assertions.assertThatThrownBy(() -> memberQueryService.getMemberScore())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                then(memberRepository).should(times(1)).findByMemberIdAndMemberDeletedAtNull(testMemberId);
            }
        }
    }
}