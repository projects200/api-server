package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerCreateRequestDto;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerCreateResponseDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerRecord;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import com.project200.undabang.timer.sync.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleTimerCommandServiceImplTest {
    private static final LocalDateTime ORIGINAL_TIME = LocalDateTime.of(2026, 10, 19, 10, 0, 0, 123_000_000);
    private final UUID memberId = UUID.randomUUID();
    private final Member member = Member.builder().memberId(memberId).build();

    @Mock
    private PolicyService policyService;
    @Mock
    private SimpleTimerRepository simpleTimerRepository;
    @Mock
    private TimerSyncService timerSyncService;
    @InjectMocks
    private SimpleTimerCommandServiceImpl simpleTimerCommandService;

    @BeforeEach
    void setUserContext() {
        UserContextHolder.setUserId(memberId);
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.reset();
    }

    @Nested
    @DisplayName("createSimpleTimer 메소드는")
    class CreateSimpleTimerTest {
        @Test
        @DisplayName("기존 요청은 빈 목록과 중복 시간도 허용하고 생성 시각을 반환한다")
        void createSimpleTimer_Success() {
            lockMember();
            given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT)).willReturn(6);
            given(simpleTimerRepository.countByMemberAndSimpleTimerDeletedAtNull(member)).willReturn(0);
            SimpleTimer savedTimer = timer(1L, 120, 100L);
            given(simpleTimerRepository.save(any(SimpleTimer.class))).willReturn(savedTimer);

            SimpleTimerCreateResponseDto response = simpleTimerCommandService.createSimpleTimer(
                    new SimpleTimerCreateRequestDto(120, null, null));

            assertThat(response.getSimpleTimerId()).isEqualTo(1L);
            assertThat(response.getUpdatedAt()).isEqualTo(ORIGINAL_TIME);
            var order = inOrder(timerSyncService, simpleTimerRepository);
            order.verify(timerSyncService).lockMember(memberId);
            order.verify(simpleTimerRepository).countByMemberAndSimpleTimerDeletedAtNull(member);
            order.verify(simpleTimerRepository).save(any(SimpleTimer.class));
            verify(timerSyncService, never()).recordCreation(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("기존 요청은 최대 개수에서 409로 거절한다")
        void createSimpleTimer_Fail_MaxCountViolation() {
            lockMember();
            given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT)).willReturn(6);
            given(simpleTimerRepository.countByMemberAndSimpleTimerDeletedAtNull(member)).willReturn(6);

            assertError(() -> simpleTimerCommandService.createSimpleTimer(new SimpleTimerCreateRequestDto(120, null, null)),
                    ErrorCode.SIMPLE_TIMER_MAX_COUNT_VIOLATION);
            verify(simpleTimerRepository, never()).save(any());
        }

        @Test
        @DisplayName("생성 재시도는 변경 삭제 정리 여부와 무관하게 원래 영수증을 먼저 반환한다")
        void replayCreationBeforeReadingTimersOrPolicies() {
            lockMember();
            UUID requestId = UUID.randomUUID();
            given(timerSyncService.findCreation(memberId, requestId, TimerType.SIMPLE))
                    .willReturn(Optional.of(new TimerCreateResult(91L, ORIGINAL_TIME)));

            SimpleTimerCreateResponseDto response = simpleTimerCommandService.createSimpleTimer(
                    new SimpleTimerCreateRequestDto(999, requestId, 1L));

            assertThat(response.getSimpleTimerId()).isEqualTo(91L);
            assertThat(response.getUpdatedAt()).isEqualTo(ORIGINAL_TIME);
            verifyNoInteractions(simpleTimerRepository, policyService);
            var order = inOrder(timerSyncService);
            order.verify(timerSyncService).lockMember(memberId);
            order.verify(timerSyncService).findCreation(memberId, requestId, TimerType.SIMPLE);
        }

        @Test
        @DisplayName("동기화 생성은 최신 기기 시각과 ID 순으로 남기고 오래된 생성 응답도 보존한다")
        void syncCreatePrunesByEditedAtThenIdAndPreservesResponse() {
            lockMember();
            UUID requestId = UUID.randomUUID();
            SimpleTimer olderCreate = timer(8L, 120, 1L);
            SimpleTimer tiedLowId = timer(2L, 120, 200L);
            SimpleTimer tiedHighId = timer(3L, 120, 200L);
            SimpleTimer latest = timer(1L, 40, 300L);
            given(simpleTimerRepository.save(any(SimpleTimer.class))).willReturn(olderCreate);
            given(simpleTimerRepository.findByMemberAndSimpleTimerDeletedAtNullOrderBySimpleTimerTimeAsc(member))
                    .willReturn(new ArrayList<>(List.of(olderCreate, tiedLowId, latest, tiedHighId)));
            given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT)).willReturn(2);

            SimpleTimerCreateResponseDto response = simpleTimerCommandService.createSimpleTimer(
                    new SimpleTimerCreateRequestDto(120, requestId, 1L));

            assertThat(response.getSimpleTimerId()).isEqualTo(8L);
            assertThat(response.getUpdatedAt()).isEqualTo(ORIGINAL_TIME);
            assertThat(olderCreate.getSimpleTimerDeletedAt()).isNotNull();
            assertThat(tiedLowId.getSimpleTimerDeletedAt()).isNotNull();
            assertThat(tiedHighId.getSimpleTimerDeletedAt()).isNull();
            assertThat(latest.getSimpleTimerDeletedAt()).isNull();
            verify(timerSyncService).recordCreation(member, requestId, TimerType.SIMPLE, 8L, ORIGINAL_TIME);
            verify(simpleTimerRepository, never()).countByMemberAndSimpleTimerDeletedAtNull(any());
        }

        @Test
        @DisplayName("업그레이드 전 행은 서버 수정 시각을 사용하며 생성 ID만 있어도 수락한다")
        void createRequestIdAloneUsesServerTimeAndHistoricalFallback() {
            lockMember();
            UUID requestId = UUID.randomUUID();
            SimpleTimer historical = timer(1L, 60, null);
            long effective = TimerSyncService.effectiveEditedAt(null, ORIGINAL_TIME);
            SimpleTimer older = timer(2L, 60, effective - 1);
            given(simpleTimerRepository.save(any(SimpleTimer.class))).willReturn(older);
            given(simpleTimerRepository.findByMemberAndSimpleTimerDeletedAtNullOrderBySimpleTimerTimeAsc(member))
                    .willReturn(new ArrayList<>(List.of(older, historical)));
            given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT)).willReturn(1);

            simpleTimerCommandService.createSimpleTimer(new SimpleTimerCreateRequestDto(60, requestId, null));

            ArgumentCaptor<SimpleTimer> captor = ArgumentCaptor.forClass(SimpleTimer.class);
            verify(simpleTimerRepository).save(captor.capture());
            assertThat(captor.getValue().getEditedAt()).isNotNull().isNotNegative();
            assertThat(historical.getSimpleTimerDeletedAt()).isNull();
            assertThat(older.getSimpleTimerDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("생성 ID 없이 기기 시각을 보내거나 음수 시각을 보내면 400이다")
        void rejectsInvalidCreateMetadata() {
            lockMember();
            assertError(() -> simpleTimerCommandService.createSimpleTimer(new SimpleTimerCreateRequestDto(60, null, 1L)),
                    ErrorCode.INVALID_INPUT_VALUE);
            assertError(() -> simpleTimerCommandService.createSimpleTimer(new SimpleTimerCreateRequestDto(60, UUID.randomUUID(), -1L)),
                    ErrorCode.INVALID_INPUT_VALUE);
            verifyNoInteractions(simpleTimerRepository, policyService);
        }
    }

    @Nested
    @DisplayName("updateSimpleTimer 메소드는")
    class UpdateSimpleTimerTest {
        @Test
        @DisplayName("기존 수정은 시간을 바꾸되 같은 시간 요청은 수정 시각을 유지하고 null을 반환한다")
        void legacyUpdateAndSameDurationNoOp() {
            lockMember();
            SimpleTimer timer = timer(1L, 180, null);
            given(simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(1L, member)).willReturn(Optional.of(timer));

            assertThat(simpleTimerCommandService.updateSimpleTimer(1L, new SimpleTimerUpdateRequestDto(180, null, null))).isNull();
            assertThat(timer.getSimpleTimerUpdatedAt()).isEqualTo(ORIGINAL_TIME);
            assertThat(timer.getEditedAt()).isNull();
            assertThat(simpleTimerCommandService.updateSimpleTimer(1L, new SimpleTimerUpdateRequestDto(300, null, null))).isNull();
            assertThat(timer.getSimpleTimerTime()).isEqualTo(300);
            assertThat(timer.getEditedAt()).isNotNull();
            assertMillis(timer.getSimpleTimerUpdatedAt());
        }

        @Test
        @DisplayName("동일 시각의 새 수정 ID는 같은 시간이어도 반영한다")
        void acceptsFreshEqualTimestampEvenWhenDurationIsUnchanged() {
            lockMember();
            UUID editId = UUID.randomUUID();
            SimpleTimer timer = timer(1L, 180, 200L);
            given(simpleTimerRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(timer));

            TimerMutationResponse response = simpleTimerCommandService.updateSimpleTimer(1L,
                    new SimpleTimerUpdateRequestDto(180, editId, 200L));

            assertThat(timer.getSimpleTimerTime()).isEqualTo(180);
            assertThat(timer.getEditedAt()).isEqualTo(200L);
            assertThat(response.updatedAt()).isEqualTo(timer.getSimpleTimerUpdatedAt());
            assertMillis(response.updatedAt());
            verify(timerSyncService).recordEdit(member, TimerType.SIMPLE, 1L, editId, TimerEditOperation.UPDATE, response.updatedAt());
        }

        @Test
        @DisplayName("영수증이 있으면 현재 타이머 조회보다 먼저 원래 성공 시각을 반환한다")
        void replayUpdateBeforeDeletedOrStaleChecks() {
            lockMember();
            UUID editId = UUID.randomUUID();
            given(timerSyncService.findEdit(memberId, TimerType.SIMPLE, 1L, editId, TimerEditOperation.UPDATE))
                    .willReturn(Optional.of(new TimerMutationResponse(ORIGINAL_TIME)));

            TimerMutationResponse response = simpleTimerCommandService.updateSimpleTimer(1L,
                    new SimpleTimerUpdateRequestDto(300, editId, 1L));

            assertThat(response.updatedAt()).isEqualTo(ORIGINAL_TIME);
            verifyNoInteractions(simpleTimerRepository);
        }

        @Test
        @DisplayName("오래된 수정은 현재 ID 시간 서버 수정 시각을 포함한 충돌이다")
        void rejectsStaleEditWithCurrentRecord() {
            lockMember();
            SimpleTimer timer = timer(1L, 180, 200L);
            given(simpleTimerRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(timer));

            assertThatThrownBy(() -> simpleTimerCommandService.updateSimpleTimer(1L,
                    new SimpleTimerUpdateRequestDto(300, UUID.randomUUID(), 199L)))
                    .isInstanceOf(TimerSyncConflictException.class)
                    .satisfies(exception -> assertThat(((TimerSyncConflictException) exception).getCurrentValue())
                            .isEqualTo(new SimpleTimerRecord(1L, 180, ORIGINAL_TIME)));
            assertThat(timer.getSimpleTimerTime()).isEqualTo(180);
            verify(timerSyncService, never()).recordEdit(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("기존 행은 서버 수정 시각으로 오래된 수정을 판정한다")
        void rejectsStaleEditOnHistoricalRow() {
            lockMember();
            SimpleTimer timer = timer(1L, 180, null);
            given(simpleTimerRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(timer));

            long older = TimerSyncService.effectiveEditedAt(null, ORIGINAL_TIME) - 1;
            assertThatThrownBy(() -> simpleTimerCommandService.updateSimpleTimer(1L,
                    new SimpleTimerUpdateRequestDto(300, UUID.randomUUID(), older)))
                    .isInstanceOf(TimerSyncConflictException.class);
        }

        @Test
        @DisplayName("삭제된 소유 타이머는 시각 비교보다 먼저 410이다")
        void deletedUpdateWinsOverStaleCheck() {
            lockMember();
            SimpleTimer timer = deletedTimer();
            given(simpleTimerRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(timer));

            assertError(() -> simpleTimerCommandService.updateSimpleTimer(1L,
                    new SimpleTimerUpdateRequestDto(300, UUID.randomUUID(), 1L)), ErrorCode.TIMER_DELETED);
            assertThat(timer.getSimpleTimerTime()).isEqualTo(180);
            verify(timerSyncService, never()).recordEdit(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("기존 수정의 누락 삭제 타 소유 타이머는 404이다")
        void legacyMissingTargetIsNotFound() {
            lockMember();
            assertError(() -> simpleTimerCommandService.updateSimpleTimer(1L,
                    new SimpleTimerUpdateRequestDto(300, null, null)), ErrorCode.SIMPLE_TIMER_NOT_FOUND);
        }

        @Test
        @DisplayName("동기화 수정의 누락 타 소유 타이머도 404이다")
        void syncMissingTargetIsNotFound() {
            lockMember();
            assertError(() -> simpleTimerCommandService.updateSimpleTimer(1L,
                    new SimpleTimerUpdateRequestDto(300, UUID.randomUUID(), 1L)), ErrorCode.SIMPLE_TIMER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteSimpleTimer 메소드는")
    class DeleteSimpleTimerTest {
        @Test
        @DisplayName("기존 삭제는 마지막 타이머도 삭제하며 null을 반환한다")
        void legacyDeleteAllowsZeroTimers() {
            lockMember();
            SimpleTimer timer = timer(1L, 180, null);
            given(simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(1L, member)).willReturn(Optional.of(timer));

            assertThat(simpleTimerCommandService.deleteSimpleTimer(1L, null)).isNull();
            assertThat(timer.getSimpleTimerDeletedAt()).isEqualTo(timer.getSimpleTimerUpdatedAt());
            assertThat(timer.getEditedAt()).isNotNull();
            assertMillis(timer.getSimpleTimerUpdatedAt());
            verify(simpleTimerRepository, never()).countByMemberAndSimpleTimerDeletedAtNull(any());
        }

        @Test
        @DisplayName("빈 삭제 메타데이터도 기존 요청으로 처리한다")
        void emptyDeleteMetadataUsesLegacyBehavior() {
            lockMember();
            given(simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(1L, member))
                    .willReturn(Optional.of(timer(1L, 180, null)));
            assertThat(simpleTimerCommandService.deleteSimpleTimer(1L, new TimerDeleteRequest(null, null))).isNull();
        }

        @Test
        @DisplayName("삭제는 오래된 기기 시각이어도 성공한다")
        void deleteWinsAgainstNewerEdit() {
            lockMember();
            UUID editId = UUID.randomUUID();
            SimpleTimer timer = timer(1L, 180, 500L);
            given(simpleTimerRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(timer));

            TimerMutationResponse response = simpleTimerCommandService.deleteSimpleTimer(1L, new TimerDeleteRequest(editId, 1L));

            assertThat(timer.getSimpleTimerDeletedAt()).isEqualTo(response.updatedAt());
            assertThat(timer.getEditedAt()).isEqualTo(1L);
            verify(timerSyncService).recordEdit(member, TimerType.SIMPLE, 1L, editId, TimerEditOperation.DELETE, response.updatedAt());
        }

        @Test
        @DisplayName("이미 삭제된 타이머의 새 삭제는 기존 삭제 시각을 보존하고 영수증을 저장한다")
        void newDeleteIdOnDeletedTimerPreservesTimestamp() {
            lockMember();
            UUID editId = UUID.randomUUID();
            SimpleTimer timer = deletedTimer();
            given(simpleTimerRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(timer));

            TimerMutationResponse response = simpleTimerCommandService.deleteSimpleTimer(1L, new TimerDeleteRequest(editId, 900L));

            assertThat(response.updatedAt()).isEqualTo(ORIGINAL_TIME);
            assertThat(timer.getEditedAt()).isEqualTo(500L);
            verify(timerSyncService).recordEdit(member, TimerType.SIMPLE, 1L, editId, TimerEditOperation.DELETE, ORIGINAL_TIME);
        }

        @Test
        @DisplayName("삭제 재시도는 타이머 상태를 읽지 않고 원래 시각을 반환한다")
        void replayDeleteBeforeReadingTimer() {
            lockMember();
            UUID editId = UUID.randomUUID();
            given(timerSyncService.findEdit(memberId, TimerType.SIMPLE, 1L, editId, TimerEditOperation.DELETE))
                    .willReturn(Optional.of(new TimerMutationResponse(ORIGINAL_TIME)));

            assertThat(simpleTimerCommandService.deleteSimpleTimer(1L, new TimerDeleteRequest(editId, 1L)).updatedAt())
                    .isEqualTo(ORIGINAL_TIME);
            verifyNoInteractions(simpleTimerRepository);
        }

        @Test
        @DisplayName("기존 삭제의 누락 삭제 타 소유 타이머는 404이다")
        void legacyMissingTargetIsNotFound() {
            lockMember();
            assertError(() -> simpleTimerCommandService.deleteSimpleTimer(1L, null), ErrorCode.SIMPLE_TIMER_NOT_FOUND);
        }

        @Test
        @DisplayName("동기화 삭제의 누락 타 소유 타이머도 404이다")
        void syncMissingTargetIsNotFound() {
            lockMember();
            assertError(() -> simpleTimerCommandService.deleteSimpleTimer(1L, new TimerDeleteRequest(UUID.randomUUID(), 1L)),
                    ErrorCode.SIMPLE_TIMER_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("수정 삭제의 메타데이터는 두 필드가 모두 있어야 하며 음수 시각은 거절한다")
    void rejectsPartialOrNegativeMutationMetadata() {
        lockMember();
        List<TimerDeleteRequest> invalid = List.of(new TimerDeleteRequest(UUID.randomUUID(), null),
                new TimerDeleteRequest(null, 1L), new TimerDeleteRequest(UUID.randomUUID(), -1L));
        for (TimerDeleteRequest request : invalid) {
            assertError(() -> simpleTimerCommandService.deleteSimpleTimer(1L, request), ErrorCode.INVALID_INPUT_VALUE);
            assertError(() -> simpleTimerCommandService.updateSimpleTimer(1L,
                    new SimpleTimerUpdateRequestDto(180, request.editId(), request.editedAt())), ErrorCode.INVALID_INPUT_VALUE);
        }
        verifyNoInteractions(simpleTimerRepository);
    }

    @Test
    @DisplayName("모든 쓰기는 회원 잠금 실패 시 타이머 정책을 조회하지 않는다")
    void allWritesRequireMemberLock() {
        given(timerSyncService.lockMember(memberId)).willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        assertError(() -> simpleTimerCommandService.createSimpleTimer(new SimpleTimerCreateRequestDto(180, null, null)), ErrorCode.MEMBER_NOT_FOUND);
        assertError(() -> simpleTimerCommandService.updateSimpleTimer(1L, new SimpleTimerUpdateRequestDto(180, null, null)), ErrorCode.MEMBER_NOT_FOUND);
        assertError(() -> simpleTimerCommandService.deleteSimpleTimer(1L, null), ErrorCode.MEMBER_NOT_FOUND);
        assertError(() -> simpleTimerCommandService.createDefaultSimpleTimer(memberId), ErrorCode.MEMBER_NOT_FOUND);
        verifyNoInteractions(simpleTimerRepository, policyService);
    }

    @Nested
    @DisplayName("createDefaultSimpleTimer 메소드는")
    class CreateDefaultSimpleTimerTest {
        @ParameterizedTest
        @CsvSource(value = {
                "6|30,40,50,60,75,90|6",
                "6|30,40,50|3",
                "3|30,40,50,60,75,90|3",
                "6| 30 , 40,  50 , 60 , 75 , 90 |6"
        }, delimiter = '|')
        @DisplayName("초기 개수와 값 개수 중 작은 만큼 공백을 제거하여 생성한다")
        void parsesDefaultsWithinPolicy(int initialCount, String values, int expectedCount) {
            lockMember();
            defaultPolicies(initialCount, values, 6);

            simpleTimerCommandService.createDefaultSimpleTimer(memberId);

            ArgumentCaptor<List<SimpleTimer>> captor = ArgumentCaptor.forClass(List.class);
            verify(simpleTimerRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(expectedCount);
            assertThat(captor.getValue().get(0).getSimpleTimerTime()).isEqualTo(30);
            assertThat(captor.getValue()).allSatisfy(timer -> {
                assertThat(timer.getMember()).isSameAs(member);
                assertThat(timer.getEditedAt()).isNotNull();
                assertMillis(timer.getSimpleTimerUpdatedAt());
            });
        }

        @Test
        @DisplayName("사용자 생성과 경합하면 기존 타이머를 보존하고 남은 자리만 채운다")
        void fillsOnlyAvailableSlotsAfterLock() {
            lockMember();
            defaultPolicies(6, "30,40,50,60,75,90", 6);
            given(simpleTimerRepository.countByMemberAndSimpleTimerDeletedAtNull(member)).willReturn(5);

            simpleTimerCommandService.createDefaultSimpleTimer(memberId);

            ArgumentCaptor<List<SimpleTimer>> captor = ArgumentCaptor.forClass(List.class);
            var order = inOrder(timerSyncService, simpleTimerRepository);
            order.verify(timerSyncService).lockMember(memberId);
            order.verify(simpleTimerRepository).countByMemberAndSimpleTimerDeletedAtNull(member);
            order.verify(simpleTimerRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting(SimpleTimer::getSimpleTimerTime).containsExactly(30);
        }

        @Test
        @DisplayName("이미 가득 찬 목록에는 초기 타이머를 추가하지 않는다")
        void fullListGetsNoDefaults() {
            lockMember();
            defaultPolicies(6, "30,40,50,60,75,90", 6);
            given(simpleTimerRepository.countByMemberAndSimpleTimerDeletedAtNull(member)).willReturn(6);

            simpleTimerCommandService.createDefaultSimpleTimer(memberId);

            ArgumentCaptor<List<SimpleTimer>> captor = ArgumentCaptor.forClass(List.class);
            verify(simpleTimerRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).isEmpty();
        }
    }

    private void lockMember() {
        given(timerSyncService.lockMember(memberId)).willReturn(member);
    }

    private void defaultPolicies(int initialCount, String values, int maxCount) {
        given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT)).willReturn(initialCount);
        given(policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES)).willReturn(values);
        given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT)).willReturn(maxCount);
    }

    private SimpleTimer timer(Long id, int time, Long editedAt) {
        return SimpleTimer.builder().id(id).member(member).simpleTimerTime(time)
                .simpleTimerCreatedAt(ORIGINAL_TIME).simpleTimerUpdatedAt(ORIGINAL_TIME).editedAt(editedAt).build();
    }

    private SimpleTimer deletedTimer() {
        return SimpleTimer.builder().id(1L).member(member).simpleTimerTime(180).editedAt(500L)
                .simpleTimerCreatedAt(ORIGINAL_TIME).simpleTimerUpdatedAt(ORIGINAL_TIME)
                .simpleTimerDeletedAt(ORIGINAL_TIME).build();
    }

    private void assertError(Runnable operation, ErrorCode errorCode) {
        assertThatThrownBy(operation::run).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertMillis(LocalDateTime timestamp) {
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getNano() % 1_000_000).isZero();
    }
}
