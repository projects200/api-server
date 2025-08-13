package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.member.dto.event.MemberSignedUpEvent;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
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
            MemberSignedUpEvent event = new MemberSignedUpEvent(testMemberId);

            // MemberRepository.findById 호출 시 testMember를 반환하도록 설정
            given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
            given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT)).willReturn(6);
            given(policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES)).willReturn("30,40,50,60,75,90");

            // when
            // Member 객체 대신 MemberSignedUpEvent 객체를 파라미터로 전달
            simpleTimerCommandService.createDefaultSimpleTimer(testMemberId);

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

        @Test
        @DisplayName("회원 정보가 존재하지 않을경우 CUSTOM_EXCEPTION 을 반환한다.")
        void createDefaultSimpleTimer_memberNotFoundException() {
            // given
            UUID testMemberId = UUID.randomUUID();
            MemberSignedUpEvent event = new MemberSignedUpEvent(testMemberId);

            // MemberRepository.findById 호출 시 Optional.empty() 반환
            given(memberRepository.findById(testMemberId)).willReturn(Optional.empty());

            // when & then
            Assertions.assertThatThrownBy(() -> simpleTimerCommandService.createDefaultSimpleTimer(testMemberId));
        }
    }

    @Nested
    @DisplayName("getTimeList 메소드는")
    class GetTimeListTest {

        @Test
        @DisplayName("정책의 타이머 개수보다 타이머 값이 적으면, 설정된 값 만큼만 타이머를 생성한다.")
        void createLessTimerThanPolicyCount() {
            // given
            UUID testMemberId = UUID.randomUUID();
            Member testMember = Member.builder().memberId(testMemberId).build();
            MemberSignedUpEvent event = new MemberSignedUpEvent(testMemberId); // MemberSignedUpEvent 객체 생성

            // MemberRepository.findById 호출 시 testMember를 반환하도록 설정
            given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
            given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT)).willReturn(6);
            given(policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES)).willReturn("30,40,50");

            // when
            // Member 객체 대신 MemberSignedUpEvent 객체를 파라미터로 전달
            simpleTimerCommandService.createDefaultSimpleTimer(testMemberId);

            // then
            ArgumentCaptor<List<SimpleTimer>> captor = ArgumentCaptor.forClass(List.class);
            verify(simpleTimerRepository).saveAll(captor.capture());
            List<SimpleTimer> savedTimers = captor.getValue();

            Assertions.assertThat(savedTimers).hasSize(3);
            Assertions.assertThat(savedTimers.get(0).getSimpleTimerTime()).isEqualTo(30);
            Assertions.assertThat(savedTimers.get(1).getSimpleTimerTime()).isEqualTo(40);
            Assertions.assertThat(savedTimers.get(2).getSimpleTimerTime()).isEqualTo(50);
        }

        @Test
        @DisplayName("정책의 타이머 개수보다 타이머 값이 많으면, 설정된 개수 만큼만 타이머를 생성한다.")
        void createMoreTimerThanPolicyCount() {
            // given
            UUID testMemberId = UUID.randomUUID();
            Member testMember = Member.builder().memberId(testMemberId).build();
            MemberSignedUpEvent event = new MemberSignedUpEvent(testMemberId); // MemberSignedUpEvent 객체 생성

            // MemberRepository.findById 호출 시 testMember를 반환하도록 설정
            given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
            given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT)).willReturn(3);
            given(policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES)).willReturn("30,40,50,60,75,90");

            // when
            // Member 객체 대신 MemberSignedUpEvent 객체를 파라미터로 전달
            simpleTimerCommandService.createDefaultSimpleTimer(testMemberId);

            // then
            ArgumentCaptor<List<SimpleTimer>> captor = ArgumentCaptor.forClass(List.class);
            verify(simpleTimerRepository).saveAll(captor.capture());
            List<SimpleTimer> savedTimers = captor.getValue();

            Assertions.assertThat(savedTimers).hasSize(3);
            Assertions.assertThat(savedTimers.get(0).getSimpleTimerTime()).isEqualTo(30);
            Assertions.assertThat(savedTimers.get(1).getSimpleTimerTime()).isEqualTo(40);
            Assertions.assertThat(savedTimers.get(2).getSimpleTimerTime()).isEqualTo(50);
        }

        @Test
        @DisplayName("타이머 값 문자열에 공백이 포함되어 있어도 정상적으로 파싱하여 타이머를 생성한다.")
        void createTimerWithWhitespaceValues() {
            // given
            UUID testMemberId = UUID.randomUUID();
            Member testMember = Member.builder().memberId(testMemberId).build();
            MemberSignedUpEvent event = new MemberSignedUpEvent(testMemberId); // MemberSignedUpEvent 객체 생성

            // MemberRepository.findById 호출 시 testMember를 반환하도록 설정
            given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
            given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT)).willReturn(6);
            given(policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES)).willReturn(" 30 , 40,  50 , 60 , 75 , 90 ");

            // when
            // Member 객체 대신 MemberSignedUpEvent 객체를 파라미터로 전달
            simpleTimerCommandService.createDefaultSimpleTimer(testMemberId);

            // then
            ArgumentCaptor<List<SimpleTimer>> captor = ArgumentCaptor.forClass(List.class);
            verify(simpleTimerRepository).saveAll(captor.capture());
            List<SimpleTimer> savedTimers = captor.getValue();

            Assertions.assertThat(savedTimers).hasSize(6);
            Assertions.assertThat(savedTimers.get(0).getSimpleTimerTime()).isEqualTo(30);
            Assertions.assertThat(savedTimers.get(1).getSimpleTimerTime()).isEqualTo(40);
            Assertions.assertThat(savedTimers.get(2).getSimpleTimerTime()).isEqualTo(50);
            Assertions.assertThat(savedTimers.get(3).getSimpleTimerTime()).isEqualTo(60);
            Assertions.assertThat(savedTimers.get(4).getSimpleTimerTime()).isEqualTo(75);
            Assertions.assertThat(savedTimers.get(5).getSimpleTimerTime()).isEqualTo(90);
        }
    }
}