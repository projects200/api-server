package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerStepCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerUpdateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerDetailResponse;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import com.project200.undabang.timer.sync.TimerCreateResult;
import com.project200.undabang.timer.sync.TimerDeleteRequest;
import com.project200.undabang.timer.sync.TimerEditOperation;
import com.project200.undabang.timer.sync.TimerMutationResponse;
import com.project200.undabang.timer.sync.TimerSyncConflictException;
import com.project200.undabang.timer.sync.TimerSyncService;
import com.project200.undabang.timer.sync.TimerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CustomTimerSynchronizationTest {
    private static final UUID MEMBER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID REQUEST_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final LocalDateTime ORIGINAL_TIME = LocalDateTime.of(2026, 10, 19, 12, 0, 0, 123_000_000);

    @Mock
    private CustomTimerRepository customTimerRepository;
    @Mock
    private CustomTimerStepRepository customTimerStepRepository;
    @Mock
    private TimerSyncService timerSyncService;
    @Mock
    private PolicyService policyService;
    @InjectMocks
    private CustomTimerCommandServiceImpl service;

    private final Member member = Member.builder().memberId(MEMBER_ID).build();

    @BeforeEach
    void setUser() {
        UserContextHolder.setUserId(MEMBER_ID);
    }

    @AfterEach
    void resetUser() {
        UserContextHolder.reset();
    }

    @Test
    @DisplayName("생성 영수증 재전송은 최초 ID와 시각을 반환하며 부모와 스텝을 다시 만들지 않는다")
    void replaysCreationBeforeLoadingTimerOrValidatingReplacementSteps() {
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        given(timerSyncService.findCreation(MEMBER_ID, REQUEST_ID, TimerType.CUSTOM))
                .willReturn(Optional.of(new TimerCreateResult(7L, ORIGINAL_TIME)));

        CustomTimerCreateResponse response = service.createCustomTimer(new CustomTimerCreateRequest("재전송", List.of(), REQUEST_ID, 1L));

        assertThat(response).isEqualTo(new CustomTimerCreateResponse(7L, ORIGINAL_TIME));
        verifyNoInteractions(customTimerRepository, customTimerStepRepository, policyService);
    }

    @Test
    @DisplayName("생성 요청 ID만 전송하면 서버 기기 시각과 영수증을 저장한다")
    void createsWithRequestIdWithoutDeviceTime() {
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        allowSteps();
        CustomTimer saved = timer(200L);
        given(customTimerRepository.save(any(CustomTimer.class))).willAnswer(invocation -> {
            CustomTimer created = invocation.getArgument(0);
            assertThat(created.getEditedAt()).isNotNull().isNotNegative();
            assertThat(created.getCustomTimerUpdatedAt().getNano() % 1_000_000).isZero();
            return saved;
        });

        CustomTimerCreateResponse response = service.createCustomTimer(new CustomTimerCreateRequest("생성", steps(), REQUEST_ID, null));

        assertThat(response.customTimerId()).isEqualTo(7L);
        assertThat(response.updatedAt()).isEqualTo(ORIGINAL_TIME);
        verify(timerSyncService).recordCreation(member, REQUEST_ID, TimerType.CUSTOM, 7L, ORIGINAL_TIME);
        verify(customTimerStepRepository).saveAll(any());
    }

    @Test
    @DisplayName("전체 교체 재전송은 삭제된 대상도 다시 조회하지 않고 스텝을 생성하지 않는다")
    void replaysReplacementWithoutReplacingSteps() {
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        given(timerSyncService.findEdit(MEMBER_ID, TimerType.CUSTOM, 7L, REQUEST_ID, TimerEditOperation.REPLACE))
                .willReturn(Optional.of(new TimerMutationResponse(ORIGINAL_TIME)));

        TimerMutationResponse response = service.updateCustomTimer(7L, new CustomTimerUpdateRequest("재전송", List.of(), REQUEST_ID, 1L));

        assertThat(response.updatedAt()).isEqualTo(ORIGINAL_TIME);
        verifyNoInteractions(customTimerRepository, customTimerStepRepository, policyService);
    }

    @Test
    @DisplayName("이름 수정 재전송은 현재 이름과 무관하게 최초 응답 시각을 반환한다")
    void replaysRenameBeforeStaleOrDeletedChecks() {
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        given(timerSyncService.findEdit(MEMBER_ID, TimerType.CUSTOM, 7L, REQUEST_ID, TimerEditOperation.RENAME))
                .willReturn(Optional.of(new TimerMutationResponse(ORIGINAL_TIME)));

        TimerMutationResponse response = service.updateCustomTimerName(7L, new CustomTimerNameUpdateRequest("재전송", REQUEST_ID, 1L));

        assertThat(response.updatedAt()).isEqualTo(ORIGINAL_TIME);
        verifyNoInteractions(customTimerRepository, customTimerStepRepository, policyService);
    }

    @Test
    @DisplayName("삭제 재전송은 최초 삭제 응답 시각을 반환한다")
    void replaysDeletionBeforeLoadingTarget() {
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        given(timerSyncService.findEdit(MEMBER_ID, TimerType.CUSTOM, 7L, REQUEST_ID, TimerEditOperation.DELETE))
                .willReturn(Optional.of(new TimerMutationResponse(ORIGINAL_TIME)));

        TimerMutationResponse response = service.deleteCustomTimer(7L, new TimerDeleteRequest(REQUEST_ID, 1L));

        assertThat(response.updatedAt()).isEqualTo(ORIGINAL_TIME);
        verifyNoInteractions(customTimerRepository, customTimerStepRepository, policyService);
    }

    @Test
    @DisplayName("오래된 전체 교체는 새 이름과 스텝을 덮어쓰지 않고 현재 전체 값을 반환한다")
    void staleReplacementReturnsCurrentAggregate() {
        assertCurrentAggregateConflict(() -> service.updateCustomTimer(7L, new CustomTimerUpdateRequest("오래된 이름", steps(), REQUEST_ID, 199L)));
    }

    @Test
    @DisplayName("오래된 이름 수정은 새 전체 교체를 덮어쓰지 않고 현재 전체 값을 반환한다")
    void staleRenameReturnsCurrentAggregate() {
        assertCurrentAggregateConflict(() -> service.updateCustomTimerName(7L, new CustomTimerNameUpdateRequest("오래된 이름", REQUEST_ID, 199L)));
    }

    @Test
    @DisplayName("같은 기기 시각의 전체 교체는 이름과 메타데이터를 bulk clear 전에 모두 설정한다")
    void equalTimestampReplacementUpdatesWholeAggregateBeforeBulkClear() {
        CustomTimer timer = ownedTimer(200L);
        allowSteps();
        doAnswer(invocation -> {
            assertThat(timer.getCustomTimerName()).isEqualTo("새 이름");
            assertThat(timer.getEditedAt()).isEqualTo(200L);
            assertThat(timer.getCustomTimerUpdatedAt().getNano() % 1_000_000).isZero();
            return null;
        }).when(customTimerStepRepository).softDeleteAllByCustomTimer(timer);

        TimerMutationResponse response = service.updateCustomTimer(7L, new CustomTimerUpdateRequest("새 이름", steps(), REQUEST_ID, 200L));

        assertThat(response.updatedAt()).isEqualTo(timer.getCustomTimerUpdatedAt());
        verify(timerSyncService).recordEdit(member, TimerType.CUSTOM, 7L, REQUEST_ID, TimerEditOperation.REPLACE, response.updatedAt());
        verify(customTimerStepRepository).saveAll(any());
    }

    @Test
    @DisplayName("같은 기기 시각의 이름 수정은 스텝을 바꾸지 않고 성공한다")
    void equalTimestampRenameKeepsSteps() {
        CustomTimer timer = ownedTimer(200L);

        TimerMutationResponse response = service.updateCustomTimerName(7L, new CustomTimerNameUpdateRequest("새 이름", REQUEST_ID, 200L));

        assertThat(timer.getCustomTimerName()).isEqualTo("새 이름");
        assertThat(timer.getEditedAt()).isEqualTo(200L);
        assertThat(response.updatedAt()).isEqualTo(timer.getCustomTimerUpdatedAt());
        verifyNoInteractions(customTimerStepRepository, policyService);
        verify(timerSyncService).recordEdit(member, TimerType.CUSTOM, 7L, REQUEST_ID, TimerEditOperation.RENAME, response.updatedAt());
    }

    @Test
    @DisplayName("삭제된 부모는 오래된 이름 수정과 전체 교체보다 우선하며 부활하지 않는다")
    void deletedTargetRejectsBothEditsBeforeTimestampComparison() {
        CustomTimer timer = ownedTimer(200L);
        timer.deleteCustomTimer(300L);

        assertThatThrownBy(() -> service.updateCustomTimerName(7L, new CustomTimerNameUpdateRequest("부활", REQUEST_ID, 1L)))
                .isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.TIMER_DELETED);
        assertThatThrownBy(() -> service.updateCustomTimer(7L, new CustomTimerUpdateRequest("부활", steps(), REQUEST_ID, 1L)))
                .isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.TIMER_DELETED);
        assertThat(timer.getCustomTimerName()).isEqualTo("현재 이름");
        assertThat(timer.getEditedAt()).isEqualTo(300L);
        verifyNoInteractions(customTimerStepRepository, policyService);
        verify(timerSyncService, never()).recordEdit(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("삭제는 오래된 기기 시각도 허용하고 부모 수정 시각도 함께 기록한다")
    void deletionWinsOverNewerEditTime() {
        CustomTimer timer = ownedTimer(200L);
        doAnswer(invocation -> {
            assertThat(timer.getCustomTimerDeletedAt()).isEqualTo(timer.getCustomTimerUpdatedAt());
            assertThat(timer.getEditedAt()).isEqualTo(1L);
            return null;
        }).when(customTimerStepRepository).softDeleteAllByCustomTimer(timer);

        TimerMutationResponse response = service.deleteCustomTimer(7L, new TimerDeleteRequest(REQUEST_ID, 1L));

        assertThat(response.updatedAt()).isEqualTo(timer.getCustomTimerDeletedAt());
        verify(timerSyncService).recordEdit(member, TimerType.CUSTOM, 7L, REQUEST_ID, TimerEditOperation.DELETE, response.updatedAt());
    }

    @Test
    @DisplayName("다른 요청 ID로 이미 삭제된 대상을 삭제하면 기존 삭제 시각의 영수증을 남긴다")
    void anotherDeleteOfDeletedTargetKeepsDeletionTime() {
        CustomTimer timer = ownedTimer(200L);
        timer.deleteCustomTimer(300L);
        LocalDateTime deletionTime = timer.getCustomTimerUpdatedAt();

        TimerMutationResponse response = service.deleteCustomTimer(7L, new TimerDeleteRequest(REQUEST_ID, 1L));

        assertThat(response.updatedAt()).isEqualTo(deletionTime);
        assertThat(timer.getEditedAt()).isEqualTo(300L);
        verifyNoInteractions(customTimerStepRepository);
        verify(timerSyncService).recordEdit(member, TimerType.CUSTOM, 7L, REQUEST_ID, TimerEditOperation.DELETE, deletionTime);
    }

    @Test
    @DisplayName("삭제된 대상의 기존 메타데이터 없는 변경과 삭제는 404를 유지한다")
    void legacyDeletedTargetRemainsNotFound() {
        CustomTimer timer = ownedTimer(200L);
        timer.deleteCustomTimer(300L);

        assertThatThrownBy(() -> service.updateCustomTimerName(7L, new CustomTimerNameUpdateRequest("이름", null, null)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
        assertThatThrownBy(() -> service.updateCustomTimer(7L, new CustomTimerUpdateRequest("이름", steps(), null, null)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
        assertThatThrownBy(() -> service.deleteCustomTimer(7L, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
        verifyNoInteractions(customTimerStepRepository, policyService);
    }

    @Test
    @DisplayName("다른 작업에 사용한 편집 ID는 대상을 변경하기 전에 거부한다")
    void rejectsReusedEditIdBeforeMutation() {
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        given(timerSyncService.findEdit(MEMBER_ID, TimerType.CUSTOM, 7L, REQUEST_ID, TimerEditOperation.REPLACE))
                .willThrow(new CustomException(ErrorCode.TIMER_REQUEST_ID_REUSED));

        assertThatThrownBy(() -> service.updateCustomTimer(7L, new CustomTimerUpdateRequest("이름", steps(), REQUEST_ID, 200L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TIMER_REQUEST_ID_REUSED);
        verifyNoInteractions(customTimerRepository, customTimerStepRepository, policyService);
    }

    @Test
    @DisplayName("이전 행에 기기 시각이 없으면 기존 서버 수정 시각으로 오래된 변경을 거부한다")
    void legacyRowUsesServerTimestampForConflict() {
        CustomTimer timer = ownedTimer(null);
        given(customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(timer)).willReturn(List.of());

        assertThatThrownBy(() -> service.updateCustomTimerName(7L, new CustomTimerNameUpdateRequest("오래된 이름", REQUEST_ID, 0L)))
                .isInstanceOf(TimerSyncConflictException.class);
        assertThat(timer.getCustomTimerName()).isEqualTo("현재 이름");
        assertThat(timer.getEditedAt()).isNull();
        verify(timerSyncService, never()).recordEdit(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("다른 타이머 종류의 생성 요청 ID는 부모와 스텝을 만들기 전에 거부한다")
    void rejectsCreateIdReusedAcrossTimerTypes() {
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        given(timerSyncService.findCreation(MEMBER_ID, REQUEST_ID, TimerType.CUSTOM))
                .willThrow(new CustomException(ErrorCode.TIMER_REQUEST_ID_REUSED));

        assertThatThrownBy(() -> service.createCustomTimer(new CustomTimerCreateRequest("이름", steps(), REQUEST_ID, 100L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TIMER_REQUEST_ID_REUSED);
        verifyNoInteractions(customTimerRepository, customTimerStepRepository, policyService);
    }

    @Test
    @DisplayName("새 동기화 요청도 존재하지 않거나 다른 회원 소유인 대상에는 기존 404를 반환한다")
    void missingOrForeignTargetRemainsNotFoundForSyncCommands() {
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        given(customTimerRepository.findByIdAndMember(7L, member)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCustomTimerName(7L, new CustomTimerNameUpdateRequest("이름", REQUEST_ID, 100L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
        assertThatThrownBy(() -> service.updateCustomTimer(7L, new CustomTimerUpdateRequest("이름", steps(), REQUEST_ID, 100L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
        assertThatThrownBy(() -> service.deleteCustomTimer(7L, new TimerDeleteRequest(REQUEST_ID, 100L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
        verifyNoInteractions(customTimerStepRepository, policyService);
        verify(timerSyncService, never()).recordEdit(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("불완전하거나 음수인 메타데이터는 잠금과 변경 전에 거부한다")
    void rejectsInvalidMetadata() {
        assertThatThrownBy(() -> service.createCustomTimer(new CustomTimerCreateRequest("이름", steps(), null, 1L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.createCustomTimer(new CustomTimerCreateRequest("이름", steps(), REQUEST_ID, -1L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.updateCustomTimerName(7L, new CustomTimerNameUpdateRequest("이름", REQUEST_ID, null)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.updateCustomTimer(7L, new CustomTimerUpdateRequest("이름", steps(), null, 1L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.deleteCustomTimer(7L, new TimerDeleteRequest(REQUEST_ID, -1L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        verifyNoInteractions(timerSyncService, customTimerRepository, customTimerStepRepository, policyService);
    }

    private void assertCurrentAggregateConflict(Runnable operation) {
        CustomTimer timer = ownedTimer(200L);
        CustomTimerStep step = CustomTimerStep.builder().id(8L).customTimer(timer)
                .customTimerStepName("현재 스텝").customTimerStepOrder((byte) 0).customTimerStepTime(60).build();
        given(customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(timer)).willReturn(List.of(step));

        assertThatThrownBy(operation::run).isInstanceOf(TimerSyncConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TIMER_EDIT_CONFLICT)
                .satisfies(exception -> {
                    CustomTimerDetailResponse current = (CustomTimerDetailResponse) ((TimerSyncConflictException) exception).getCurrentValue();
                    assertThat(current.getCustomTimerId()).isEqualTo(7L);
                    assertThat(current.getCustomTimerName()).isEqualTo("현재 이름");
                    assertThat(current.getUpdatedAt()).isEqualTo(ORIGINAL_TIME);
                    assertThat(current.getCustomTimerStepCount()).isEqualTo(1);
                    assertThat(current.getCustomTimerSteps().getFirst().customTimerStepId()).isEqualTo(8L);
                    assertThat(current.getCustomTimerSteps().getFirst().customTimerStepName()).isEqualTo("현재 스텝");
                    assertThat(current.getCustomTimerSteps().getFirst().customTimerStepTime()).isEqualTo(60);
                });
        assertThat(timer.getCustomTimerName()).isEqualTo("현재 이름");
        assertThat(timer.getEditedAt()).isEqualTo(200L);
        verify(customTimerStepRepository, never()).softDeleteAllByCustomTimer(any());
        verify(customTimerStepRepository, never()).saveAll(any());
        verify(timerSyncService, never()).recordEdit(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(policyService);
    }

    private CustomTimer ownedTimer(Long editedAt) {
        CustomTimer timer = timer(editedAt);
        given(timerSyncService.lockMember(MEMBER_ID)).willReturn(member);
        given(customTimerRepository.findByIdAndMember(7L, member)).willReturn(Optional.of(timer));
        return timer;
    }

    private CustomTimer timer(Long editedAt) {
        return CustomTimer.builder().id(7L).member(member).customTimerName("현재 이름")
                .customTimerUpdatedAt(ORIGINAL_TIME).editedAt(editedAt).build();
    }

    private List<CustomTimerStepCreateRequest> steps() {
        return List.of(new CustomTimerStepCreateRequest("스텝", (byte) 0, 60));
    }

    private void allowSteps() {
        given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT)).willReturn(1);
        given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MAX_COUNT)).willReturn(50);
    }
}
