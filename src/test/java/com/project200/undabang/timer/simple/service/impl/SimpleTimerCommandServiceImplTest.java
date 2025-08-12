package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SimpleTimerCommandServiceImplTest {

    @Mock
    private PolicyService policyService;

    @Mock
    private SimpleTimerRepository simpleTimerRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private SimpleTimerCommandServiceImpl simpleTimerCommandService;

    @Nested
    @DisplayName("createDefaultSimpleTimer 메소드는")
    class createDefaultSimpleTimerTest {

        @Test
        @DisplayName("정상적인 요청이 들어오면 심플 타이머를 생성한다.")
        void createDefaultSimpleTimer() {
            // given
            UUID testMemberId = UUID.randomUUID();
            Member testMember = Member.builder().memberId(testMemberId).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testMemberId);
                given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT)).willReturn(6);
                given(policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES)).willReturn("30,40,50,60,75,90");
                given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));

                // when
                simpleTimerCommandService.createDefaultSimpleTimer();

                // then
                ArgumentCaptor<List<SimpleTimer>> captor = ArgumentCaptor.forClass(List.class);
                verify(simpleTimerRepository).saveAll(captor.capture());
                List<SimpleTimer> savedTimers = captor.getValue();

                Assertions.assertThat(savedTimers).hasSize(6);
                Assertions.assertThat(savedTimers.get(0).getMember()).isEqualTo(testMember);
                Assertions.assertThat(savedTimers.get(0).getSimpleTimerTime()).isEqualTo(30);
                Assertions.assertThat(savedTimers.get(1).getSimpleTimerTime()).isEqualTo(40);
                Assertions.assertThat(savedTimers.get(2).getSimpleTimerTime()).isEqualTo(50);
                Assertions.assertThat(savedTimers.get(3).getSimpleTimerTime()).isEqualTo(60);
                Assertions.assertThat(savedTimers.get(4).getSimpleTimerTime()).isEqualTo(75);
                Assertions.assertThat(savedTimers.get(5).getSimpleTimerTime()).isEqualTo(90);
            }
        }

        @Test
        @DisplayName("정책의 타이머 개수와 실제 값의 개수가 다르면 예외를 발생시킨다.")
        void throwsException_whenPolicyMismatch() {
            // given
            UUID testMemberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testMemberId);
                given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT)).willReturn(6);
                given(policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES)).willReturn("30,40,50");

                // when & then
                Assertions.assertThatThrownBy(() -> simpleTimerCommandService.createDefaultSimpleTimer())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외를 발생시킨다.")
        void throwsException_whenMemberNotFound() {
            // given
            UUID testMemberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testMemberId);
                given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT)).willReturn(6);
                given(policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES)).willReturn("30,40,50,60,75,90");
                given(memberRepository.findById(testMemberId)).willReturn(Optional.empty());

                // when & then
                Assertions.assertThatThrownBy(() -> simpleTimerCommandService.createDefaultSimpleTimer())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

}