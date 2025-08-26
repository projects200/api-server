package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerStepCreateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomTimerCommandServiceImplTest {

    @Mock
    private CustomTimerRepository customTimerRepository;

    @Mock
    private CustomTimerStepRepository customTimerStepRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PolicyService policyService;

    @InjectMocks
    private CustomTimerCommandServiceImpl customTimerCommandService;

    private CustomTimer createCustomTimer(Long timerId, Member member, String initialName) {
        return CustomTimer.builder()
                .id(timerId)
                .member(member)
                .customTimerName(initialName)
                .build();
    }

    @Nested
    @DisplayName("deleteCustomTimer() 메소드는")
    class Describe_deleteCustomTimer {

        @Test
        @DisplayName("유효한 회원과 타이머 ID가 주어지면 타이머 삭제 로직을 올바르게 호출한다.")
        void shouldDeleteTimerAndSteps_whenValidRequest() {
            // given
            UUID userId = UUID.randomUUID();
            Member member = Member.builder().memberId(userId).build();
            Long timerId = 1L;
            CustomTimer timer = CustomTimer.builder().id(timerId).member(member).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findByIdAndMemberAndCustomTimerDeletedAtNull(timerId, member))
                        .willReturn(Optional.of(timer));
                // when
                customTimerCommandService.deleteCustomTimer(timerId);

                // then
                verify(customTimerStepRepository, times(1)).softDeleteAllByCustomTimer(timer);
                // saveAll, save 호출 검증은 제거
            }
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 CustomException(MEMBER_NOT_FOUND)을 던진다")
        void shouldThrowException_whenMemberNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            Long timerId = 1L;

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.deleteCustomTimer(timerId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                verify(customTimerRepository, never()).findByIdAndMemberAndCustomTimerDeletedAtNull(any(), any());
            }
        }

        @Test
        @DisplayName("존재하지 않는 타이머면 CustomException(CUSTOM_TIMER_NOT_FOUND)을 던진다")
        void shouldThrowException_whenTimerNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            Member member = Member.builder().memberId(userId).build();
            Long timerId = 1L;

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findByIdAndMemberAndCustomTimerDeletedAtNull(timerId, member))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.deleteCustomTimer(timerId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);

                verify(customTimerStepRepository, never()).softDeleteAllByCustomTimer(any());
            }
        }
    }

    private CustomTimerNameUpdateRequest createUpdateRequest(String newName) {
        return new CustomTimerNameUpdateRequest(newName);
    }

    @Nested
    @DisplayName("updateCustomTimerName() 메소드는")
    class Describe_updateCustomTimerName {

        @Test
        @DisplayName("유효한 요청이 주어지면, 타이머 이름을 성공적으로 변경한다")
        void shouldUpdateTimerName_whenRequestIsValid() {
            // given
            Long timerId = 1L;
            UUID userId = UUID.randomUUID();
            Member member = createTestUser(userId); // 기존 헬퍼 메서드 사용
            CustomTimerNameUpdateRequest request = createUpdateRequest("새로운 타이머 이름");
            CustomTimer originalTimer = createCustomTimer(timerId, member, "기존 타이머 이름");

            // 엔티티의 실제 메소드 호출을 검증하기 위해 Spy 객체 생성
            CustomTimer spiedTimer = spy(originalTimer);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(userId);

                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findByIdAndMemberAndCustomTimerDeletedAtNull(timerId, member))
                        .willReturn(Optional.of(spiedTimer));

                // when
                customTimerCommandService.updateCustomTimerName(timerId, request);

                // then
                ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
                verify(spiedTimer, times(1)).updateCustomTimerName(nameCaptor.capture());
                assertThat(nameCaptor.getValue()).isEqualTo(request.getCustomTimerName());
            }
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 요청하면, CustomException(MEMBER_NOT_FOUND)을 던진다")
        void shouldThrowException_whenMemberNotFound() {
            // given
            Long timerId = 1L;
            UUID userId = UUID.randomUUID();
            CustomTimerNameUpdateRequest request = createUpdateRequest("새로운 타이머 이름");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.updateCustomTimerName(timerId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                verify(customTimerRepository, never()).findByIdAndMemberAndCustomTimerDeletedAtNull(any(), any());
            }
        }

        @Test
        @DisplayName("회원의 소유가 아닌 타이머 ID로 요청하면, CustomException(CUSTOM_TIMER_NOT_FOUND)을 던진다")
        void shouldThrowException_whenTimerNotFoundOrNotOwned() {
            // given
            Long timerId = 1L;
            UUID userId = UUID.randomUUID();
            Member member = createTestUser(userId);
            CustomTimerNameUpdateRequest request = createUpdateRequest("새로운 타이머 이름");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findByIdAndMemberAndCustomTimerDeletedAtNull(timerId, member))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.updateCustomTimerName(timerId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
            }
        }
    }

    private Member createTestUser(UUID userId) {
        return Member.builder().memberId(userId).build();
    }

    private CustomTimerCreateRequest createRequest(int stepCount) {
        List<CustomTimerStepCreateRequest> steps = IntStream.range(0, stepCount)
                .mapToObj(i -> new CustomTimerStepCreateRequest(
                        "스텝 " + i,
                        (byte) i,
                        60
                ))
                .toList();
        return new CustomTimerCreateRequest("테스트 타이머", steps);
    }

    @Nested
    @DisplayName("createCustomTimer() 메소드는")
    class Describe_createCustomTimer {

        @Test
        @DisplayName("유효한 요청이 들어오면, 커스텀 타이머와 스텝들을 성공적으로 생성한다")
        void shouldCreateCustomTimerAndSteps_whenRequestIsValid() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testUser = createTestUser(testUserId);
            CustomTimerCreateRequest request = createRequest(3);
            CustomTimer mockSavedTimer = CustomTimer.builder().id(1L).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUser));
                given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT)).willReturn(1);
                given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MAX_COUNT)).willReturn(10);
                given(customTimerRepository.save(any(CustomTimer.class))).willReturn(mockSavedTimer);

                // when
                CustomTimerCreateResponse response = customTimerCommandService.createCustomTimer(request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.customTimerId()).isEqualTo(1L);

                verify(policyService, times(1)).getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT);
                verify(policyService, times(1)).getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MAX_COUNT);

                ArgumentCaptor<CustomTimer> timerCaptor = ArgumentCaptor.forClass(CustomTimer.class);
                verify(customTimerRepository, times(1)).save(timerCaptor.capture());
                assertThat(timerCaptor.getValue().getMember()).isEqualTo(testUser);
                assertThat(timerCaptor.getValue().getCustomTimerName()).isEqualTo(request.getCustomTimerName());

                ArgumentCaptor<List<CustomTimerStep>> stepsCaptor = ArgumentCaptor.forClass(List.class);
                verify(customTimerStepRepository, times(1)).saveAll(stepsCaptor.capture());
                assertThat(stepsCaptor.getValue()).hasSize(3);
            }
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 CustomException(MEMBER_NOT_FOUND) 예외를 던진다")
        void shouldThrowException_whenMemberNotFound() {
            // given
            UUID testUserId = UUID.randomUUID();
            CustomTimerCreateRequest request = createRequest(1);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.createCustomTimer(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                verify(customTimerRepository, never()).save(any());
                verify(customTimerStepRepository, never()).saveAll(any());
            }
        }

        @Test
        @DisplayName("타이머 스텝 개수가 정책상 최소치보다 적으면 CustomException을 던진다")
        void shouldThrowException_whenStepCountIsBelowMin() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testUser = createTestUser(testUserId);
            CustomTimerCreateRequest request = createRequest(1);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUser));
                given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT)).willReturn(2); // 정책: 최소 2개

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.createCustomTimer(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_STEP_MIN_COUNT_VIOLATION);

                verify(customTimerRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("타이머 스텝 개수가 정책상 최대치를 초과하면 CustomException을 던진다")
        void shouldThrowException_whenStepCountExceedsMax() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testUser = createTestUser(testUserId);
            CustomTimerCreateRequest request = createRequest(5);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUser));
                given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT)).willReturn(1);
                given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MAX_COUNT)).willReturn(3); // 정책: 최대 3개

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.createCustomTimer(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_STEP_MAX_COUNT_VIOLATION);

                verify(customTimerRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("타이머 스텝 리스트가 order 필드 기준으로 정렬되어 있지 않으면 CustomException을 던진다")
        void shouldThrowException_whenStepListIsNotSortedByOrder() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testUser = createTestUser(testUserId);

            // 순서는 1,2,3으로 구성되어 있지만 리스트 내 순서가 뒤섞인 경우
            List<CustomTimerStepCreateRequest> unsortedSteps = List.of(
                    new CustomTimerStepCreateRequest("스텝 1", (byte) 0, 60),
                    new CustomTimerStepCreateRequest("스텝 3", (byte) 2, 60), // << 순서가 맞지 않음
                    new CustomTimerStepCreateRequest("스텝 2", (byte) 1, 30)
            );
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("정렬 안된 타이머", unsortedSteps);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUser));

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.createCustomTimer(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_STEP_ORDER_INVALID);

                verify(customTimerRepository, never()).save(any());
                verify(customTimerStepRepository, never()).saveAll(any());
            }
        }
    }

    @Nested
    @DisplayName("updateCustomTimer() 메소드는")
    class Describe_updateCustomTimer {

        @Test
        @DisplayName("유효한 요청이 주어지면, 타이머 이름 변경 및 스텝 교체를 성공적으로 수행한다")
        void shouldUpdateNameAndReplaceSteps_whenRequestIsValid() {
            // given
            UUID userId = UUID.randomUUID();
            Member member = createTestUser(userId);
            Long timerId = 1L;
            CustomTimerCreateRequest request = createRequest(2); // 2개의 새 스텝으로 교체 요청

            // 엔티티의 실제 메소드 호출을 검증하기 위해 Spy 객체 사용
            CustomTimer originalTimer = spy(CustomTimer.builder()
                    .id(timerId)
                    .member(member)
                    .customTimerName("오래된 이름")
                    .build());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(userId);

                // --- Mocking ---
                // 1. 사용자 및 타이머 조회 성공
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findByIdAndMemberAndCustomTimerDeletedAtNull(timerId, member))
                        .willReturn(Optional.of(originalTimer));
                // 2. 유효성 검사 통과를 위한 정책 모킹
                given(policyService.getPolicyValueAsInt(any(PolicyKey.class))).willReturn(1).willReturn(10);

                // when
                customTimerCommandService.updateCustomTimer(timerId, request);

                // then
                // 1. 타이머 이름이 올바르게 업데이트 되었는지 검증
                verify(originalTimer, times(1)).updateCustomTimerName(request.getCustomTimerName());

                // 2. 기존 스텝들이 삭제 처리 되었는지 검증
                verify(customTimerStepRepository, times(1)).softDeleteAllByCustomTimer(originalTimer);

                // 3. 새로운 스텝들이 저장 처리 되었는지 검증
                ArgumentCaptor<List<CustomTimerStep>> stepsCaptor = ArgumentCaptor.forClass(List.class);
                verify(customTimerStepRepository, times(1)).saveAll(stepsCaptor.capture());
                assertThat(stepsCaptor.getValue()).hasSize(2); // 요청한 대로 2개의 스텝이 생성되었는지 확인
                assertThat(stepsCaptor.getValue().get(0).getCustomTimerStepName()).isEqualTo("스텝 0");
            }
        }

        @Test
        @DisplayName("스텝 개수가 정책상 최소치보다 적으면 CustomException을 던지고 아무 작업도 수행하지 않는다")
        void shouldThrowExceptionAndDoNothing_whenStepCountIsBelowMin() {
            // given
            UUID userId = UUID.randomUUID();
            Member member = createTestUser(userId);
            Long timerId = 1L;
            CustomTimerCreateRequest request = createRequest(1); // 정책 위반 (2개 미만)
            CustomTimer timer = spy(CustomTimer.builder().id(timerId).member(member).build());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findByIdAndMemberAndCustomTimerDeletedAtNull(timerId, member))
                        .willReturn(Optional.of(timer));
                given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT)).willReturn(2); // 정책: 최소 2개

                // when & then
                assertThatThrownBy(() -> customTimerCommandService.updateCustomTimer(timerId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_STEP_MIN_COUNT_VIOLATION);

                // 예외 발생 후 DB 변경 작업이 전혀 호출되지 않았는지 검증 (중요)
                verify(timer, never()).updateCustomTimerName(anyString());
                verify(customTimerStepRepository, never()).softDeleteAllByCustomTimer(any());
                verify(customTimerStepRepository, never()).saveAll(any());
            }
        }
    }
}