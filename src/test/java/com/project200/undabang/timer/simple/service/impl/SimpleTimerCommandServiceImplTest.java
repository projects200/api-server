package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.event.MemberSignedUpEvent;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

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
    @DisplayName("deleteSimpleTimer 메소드는")
    class DeleteSimpleTimerTest {

        @Test
        @DisplayName("정상적인 요청 시 타이머를 성공적으로 논리적 삭제한다")
        void deleteSimpleTimer_Success() {
            // given
            Long simpleTimerId = 1L;
            UUID memberId = UUID.randomUUID();
            Member member = Member.builder().memberId(memberId).build();
            SimpleTimer timer = SimpleTimer.builder().id(simpleTimerId).member(member).simpleTimerTime(30).build();

            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                // 삭제 가능한 타이머가 2개 이상 있다고 가정
                given(simpleTimerRepository.countDistinctByMemberAndSimpleTimerDeletedAtNull(member)).willReturn(2);
                given(simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(simpleTimerId, member)).willReturn(Optional.of(timer));

                // when
                simpleTimerCommandService.deleteSimpleTimer(simpleTimerId);

                // then
                then(simpleTimerRepository).should().findByIdAndMemberAndSimpleTimerDeletedAtNull(simpleTimerId, member);
                assertThat(timer.getSimpleTimerDeletedAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("요청한 사용자가 존재하지 않으면 CustomException(MEMBER_NOT_FOUND)을 발생시킨다")
        void deleteSimpleTimer_Fail_MemberNotFound() {
            // given
            Long simpleTimerId = 1L;
            UUID memberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> simpleTimerCommandService.deleteSimpleTimer(simpleTimerId))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

                then(simpleTimerRepository).should(never()).countDistinctByMemberAndSimpleTimerDeletedAtNull(any(Member.class));
                then(simpleTimerRepository).should(never()).findByIdAndMemberAndSimpleTimerDeletedAtNull(anyLong(), any(Member.class));
            }
        }

        @Test
        @DisplayName("남은 타이머가 0개일 때 삭제를 시도하면 SIMPLE_TIMER_MIN_COUNT_VIOLATION을 발생시킨다")
        void deleteSimpleTimer_Fail_LastTimer() {
            // given
            Long simpleTimerId = 1L;
            UUID memberId = UUID.randomUUID();
            Member member = Member.builder().memberId(memberId).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                // 회원의 타이머 개수가 0개라고 가정
                given(simpleTimerRepository.countDistinctByMemberAndSimpleTimerDeletedAtNull(member)).willReturn(0);

                // when & then
                assertThatThrownBy(() -> simpleTimerCommandService.deleteSimpleTimer(simpleTimerId))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.SIMPLE_TIMER_MIN_COUNT_VIOLATION.getMessage());

                // 타이머를 찾는 로직이나 삭제 로직이 호출되지 않았는지 검증
                then(simpleTimerRepository).should(never()).findByIdAndMemberAndSimpleTimerDeletedAtNull(anyLong(), any(Member.class));
            }
        }

        @Test
        @DisplayName("삭제하려는 타이머가 존재하지 않으면 SIMPLE_TIMER_NOT_EXIST 예외를 발생시킨다")
        void deleteSimpleTimer_Fail_TimerNotExist() {
            // given
            Long nonExistentTimerId = 999L;
            UUID memberId = UUID.randomUUID();
            Member member = Member.builder().memberId(memberId).build();

            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                // 삭제 가능한 타이머가 2개 이상 있다고 가정
                given(simpleTimerRepository.countDistinctByMemberAndSimpleTimerDeletedAtNull(member)).willReturn(2);
                given(simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(nonExistentTimerId, member)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> simpleTimerCommandService.deleteSimpleTimer(nonExistentTimerId))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.SIMPLE_TIMER_NOT_EXIST.getMessage());

                then(simpleTimerRepository).should().findByIdAndMemberAndSimpleTimerDeletedAtNull(nonExistentTimerId, member);
            }
        }
    }

    @Nested
    @DisplayName("updateSimpleTimer 메소드는")
    class UpdateSimpleTimerTest {

        @Test
        @DisplayName("정상적인 요청 시 타이머 시간을 성공적으로 수정한다")
        void updateSimpleTimer_Success() {
            // given
            Long simpleTimerId = 1L;
            int newTime = 300;
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(newTime);

            UUID memberId = UUID.randomUUID();
            Member member = Member.builder().memberId(memberId).build();
            SimpleTimer existingTimer = SimpleTimer.of(member, 180);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(simpleTimerId, member))
                        .willReturn(Optional.of(existingTimer));

                // when
                simpleTimerCommandService.updateSimpleTimer(simpleTimerId, requestDto);

                // then
                assertThat(existingTimer.getSimpleTimerTime()).isEqualTo(newTime);
                then(memberRepository).should().findById(memberId);
                then(simpleTimerRepository).should().findByIdAndMemberAndSimpleTimerDeletedAtNull(simpleTimerId, member);
            }
        }

        @Test
        @DisplayName("동일한 시간으로 수정 요청 시 타이머 시간이 변경되지 않는다")
        void updateSimpleTimer_NoChangeWhenTimeIsSame() {
            // given
            Long simpleTimerId = 1L;
            int existingTime = 180;
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(existingTime);

            UUID memberId = UUID.randomUUID();
            Member member = Member.builder().memberId(memberId).build();
            SimpleTimer existingTimer = SimpleTimer.of(member, existingTime);
            // 원본 업데이트 시간을 저장
            LocalDateTime originalUpdatedAt = existingTimer.getSimpleTimerUpdatedAt();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(simpleTimerId, member))
                        .willReturn(Optional.of(existingTimer));

                // when
                simpleTimerCommandService.updateSimpleTimer(simpleTimerId, requestDto);

                // then
                // 시간이 동일하므로, 엔티티의 시간과 업데이트 시간은 변경되지 않아야 함
                assertThat(existingTimer.getSimpleTimerTime()).isEqualTo(existingTime);
                assertThat(existingTimer.getSimpleTimerUpdatedAt()).isEqualTo(originalUpdatedAt);
                then(memberRepository).should().findById(memberId);
                then(simpleTimerRepository).should().findByIdAndMemberAndSimpleTimerDeletedAtNull(simpleTimerId, member);
            }
        }

        @Test
        @DisplayName("수정하려는 타이머가 존재하지 않으면 CustomException(SIMPLE_TIMER_NOT_EXIST)을 발생시킨다")
        void updateSimpleTimer_Fail_TimerNotExist() {
            // given
            Long nonExistentTimerId = 999L;
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(300);
            UUID memberId = UUID.randomUUID();
            Member member = Member.builder().memberId(memberId).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(nonExistentTimerId, member))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> simpleTimerCommandService.updateSimpleTimer(nonExistentTimerId, requestDto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.SIMPLE_TIMER_NOT_EXIST.getMessage());
            }
        }

        @Test
        @DisplayName("요청한 사용자가 존재하지 않으면 CustomException(MEMBER_NOT_FOUND)을 발생시킨다")
        void updateSimpleTimer_Fail_MemberNotFound() {
            // given
            Long simpleTimerId = 1L;
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(300);
            UUID memberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> simpleTimerCommandService.updateSimpleTimer(simpleTimerId, requestDto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

                then(simpleTimerRepository).should(never()).findByIdAndMemberAndSimpleTimerDeletedAtNull(anyLong(), any(Member.class));
            }
        }
    }

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