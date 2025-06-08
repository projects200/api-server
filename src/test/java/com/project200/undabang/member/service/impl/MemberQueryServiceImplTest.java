package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
}