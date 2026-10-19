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
import com.project200.undabang.timer.custom.dto.response.CustomTimerStepRecord;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import com.project200.undabang.timer.sync.TimerCreateResult;
import com.project200.undabang.timer.sync.TimerDeleteRequest;
import com.project200.undabang.timer.sync.TimerEditOperation;
import com.project200.undabang.timer.sync.TimerMutationResponse;
import com.project200.undabang.timer.sync.TimerSyncConflictException;
import com.project200.undabang.timer.sync.TimerSyncService;
import com.project200.undabang.timer.sync.TimerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomTimerCommandServiceImpl implements CustomTimerCommandService {
    private final CustomTimerRepository customTimerRepository;
    private final CustomTimerStepRepository customTimerStepRepository;
    private final TimerSyncService timerSyncService;
    private final PolicyService policyService;

    @Override
    public CustomTimerCreateResponse createCustomTimer(CustomTimerCreateRequest dto) {
        TimerSyncService.validateCreateMetadata(dto.getCreateRequestId(), dto.getEditedAt());
        Member member = timerSyncService.lockMember(UserContextHolder.getUserId());

        if (dto.getCreateRequestId() != null) {
            Optional<TimerCreateResult> replay = timerSyncService.findCreation(member.getMemberId(), dto.getCreateRequestId(), TimerType.CUSTOM);
            if (replay.isPresent()) {
                return new CustomTimerCreateResponse(replay.get().timerId(), replay.get().updatedAt());
            }
        }

        validateCustomTimerSteps(dto.getCustomTimerSteps());
        long editedAt = dto.getEditedAt() != null ? dto.getEditedAt() : System.currentTimeMillis();
        CustomTimer customTimer = CustomTimer.of(member, dto.getCustomTimerName(), editedAt);
        CustomTimer savedTimer = customTimerRepository.save(customTimer);
        createStepTimerList(savedTimer, dto.getCustomTimerSteps());

        if (dto.getCreateRequestId() != null) {
            timerSyncService.recordCreation(member, dto.getCreateRequestId(), TimerType.CUSTOM, savedTimer.getId(), savedTimer.getCustomTimerUpdatedAt());
        }
        return new CustomTimerCreateResponse(savedTimer.getId(), savedTimer.getCustomTimerUpdatedAt());
    }

    @Override
    public TimerMutationResponse deleteCustomTimer(Long customTimerId, TimerDeleteRequest request) {
        UUID editId = request != null ? request.editId() : null;
        Long editedAt = request != null ? request.editedAt() : null;
        boolean sync = TimerSyncService.isSyncEdit(editId, editedAt);
        Member member = timerSyncService.lockMember(UserContextHolder.getUserId());
        if (sync) {
            Optional<TimerMutationResponse> replay = timerSyncService.findEdit(member.getMemberId(), TimerType.CUSTOM, customTimerId, editId, TimerEditOperation.DELETE);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        CustomTimer customTimer = getCustomTimer(member, customTimerId);
        if (customTimer.getCustomTimerDeletedAt() != null) {
            if (!sync) {
                throw new CustomException(ErrorCode.CUSTOM_TIMER_NOT_FOUND);
            }
        } else {
            // 삭제는 기기 시각과 무관하게 우선한다. 부모의 모든 변경은 벌크 flush/clear 전에 설정한다.
            customTimer.deleteCustomTimer(sync ? editedAt : System.currentTimeMillis());
            customTimerStepRepository.softDeleteAllByCustomTimer(customTimer);
        }

        return recordEdit(member, customTimer, editId, TimerEditOperation.DELETE, sync);
    }

    @Override
    public TimerMutationResponse updateCustomTimer(Long customTimerId, CustomTimerUpdateRequest request) {
        boolean sync = TimerSyncService.isSyncEdit(request.getEditId(), request.getEditedAt());
        Member member = timerSyncService.lockMember(UserContextHolder.getUserId());
        if (sync) {
            Optional<TimerMutationResponse> replay = timerSyncService.findEdit(member.getMemberId(), TimerType.CUSTOM, customTimerId, request.getEditId(), TimerEditOperation.REPLACE);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        CustomTimer customTimer = getCustomTimer(member, customTimerId);
        validateUpdate(customTimer, sync, request.getEditedAt());
        validateCustomTimerSteps(request.getCustomTimerSteps());

        // 이름과 동기화 필드를 모두 먼저 변경한다. 스텝 벌크 삭제는 부모를 flush한 뒤 detach한다.
        customTimer.updateCustomTimerName(request.getCustomTimerName(), sync ? request.getEditedAt() : System.currentTimeMillis());
        customTimerStepRepository.softDeleteAllByCustomTimer(customTimer);
        createStepTimerList(customTimer, request.getCustomTimerSteps());

        return recordEdit(member, customTimer, request.getEditId(), TimerEditOperation.REPLACE, sync);
    }

    @Override
    public TimerMutationResponse updateCustomTimerName(Long customTimerId, CustomTimerNameUpdateRequest request) {
        boolean sync = TimerSyncService.isSyncEdit(request.getEditId(), request.getEditedAt());
        Member member = timerSyncService.lockMember(UserContextHolder.getUserId());
        if (sync) {
            Optional<TimerMutationResponse> replay = timerSyncService.findEdit(member.getMemberId(), TimerType.CUSTOM, customTimerId, request.getEditId(), TimerEditOperation.RENAME);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        CustomTimer customTimer = getCustomTimer(member, customTimerId);
        validateUpdate(customTimer, sync, request.getEditedAt());
        customTimer.updateCustomTimerName(request.getCustomTimerName(), sync ? request.getEditedAt() : System.currentTimeMillis());
        return recordEdit(member, customTimer, request.getEditId(), TimerEditOperation.RENAME, sync);
    }

    private void validateUpdate(CustomTimer timer, boolean sync, Long editedAt) {
        if (timer.getCustomTimerDeletedAt() != null) {
            throw new CustomException(sync ? ErrorCode.TIMER_DELETED : ErrorCode.CUSTOM_TIMER_NOT_FOUND);
        }
        if (sync && TimerSyncService.effectiveEditedAt(timer.getEditedAt(), timer.getCustomTimerUpdatedAt()) > editedAt) {
            List<CustomTimerStepRecord> steps = customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(timer)
                    .stream()
                    .map(step -> new CustomTimerStepRecord(step.getId(), step.getCustomTimerStepName(), step.getCustomTimerStepOrder(), step.getCustomTimerStepTime()))
                    .toList();
            throw new TimerSyncConflictException(CustomTimerDetailResponse.from(timer, steps));
        }
    }

    private TimerMutationResponse recordEdit(Member member, CustomTimer timer, UUID editId, TimerEditOperation operation, boolean sync) {
        if (!sync) {
            return null;
        }
        // 벌크 clear 뒤에도 같은 트랜잭션에서 기존 부모/회원 ID를 참조하는 영수증을 저장한다.
        timerSyncService.recordEdit(member, TimerType.CUSTOM, timer.getId(), editId, operation, timer.getCustomTimerUpdatedAt());
        return new TimerMutationResponse(timer.getCustomTimerUpdatedAt());
    }

    /**
     * 스텝 관련 유효성 검사를 하나의 메서드로 묶음
     * create, update에서 재활용 가능한 형태로 변경
     */
    private void validateCustomTimerSteps(List<CustomTimerStepCreateRequest> steps) {
        validateCustomTimerStepOrder(steps);
        checkCustomTimerCountLimit(steps);
    }

    /**
     * 커스텀 타이머 스텝의 순서가 정확한지 검증합니다.
     */
    private void validateCustomTimerStepOrder(List<CustomTimerStepCreateRequest> request) {
        for (int i = 0; i < request.size(); i++) {
            if (request.get(i).getCustomTimerStepOrder() != i) {
                throw new CustomException(ErrorCode.CUSTOM_TIMER_STEP_ORDER_INVALID);
            }
        }
    }

    /**
     * 커스텀 타이머 스텝의 개수가 정책에서 정의한 범위 내에 있는지 검증합니다.
     */
    private void checkCustomTimerCountLimit(List<CustomTimerStepCreateRequest> request) {
        int minStepCount = policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT);
        int maxStepCount = policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MAX_COUNT);

        if (request.size() < minStepCount) {
            throw new CustomException(ErrorCode.CUSTOM_TIMER_STEP_MIN_COUNT_VIOLATION);
        }
        if (request.size() > maxStepCount) {
            throw new CustomException(ErrorCode.CUSTOM_TIMER_STEP_MAX_COUNT_VIOLATION);
        }
    }

    /**
     * DTO 리스트를 {@link CustomTimerStep} 엔티티 리스트로 변환하고 데이터베이스에 일괄 저장합니다.
     */
    private void createStepTimerList(CustomTimer customTimer, List<CustomTimerStepCreateRequest> requestList) {
        List<CustomTimerStep> customTimerStepList = requestList.stream()
                .map(req -> req.toEntity(customTimer))
                .toList();

        customTimerStepRepository.saveAll(customTimerStepList);
    }

    /**
     * 주어진 사용자와 타이머 ID를 기반으로 커스텀 타이머를 조회합니다.
     * 타이머가 존재하지 않거나 다른 회원 소유이면 {@link CustomException}을 발생시킵니다.
     */
    private CustomTimer getCustomTimer(Member member, Long customTimerId) {
        return customTimerRepository.findByIdAndMember(customTimerId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_TIMER_NOT_FOUND));
    }
}
