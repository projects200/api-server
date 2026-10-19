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
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import com.project200.undabang.timer.sync.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SimpleTimerCommandServiceImpl implements SimpleTimerCommandService {
    private final PolicyService policyService;
    private final SimpleTimerRepository simpleTimerRepository;
    private final TimerSyncService timerSyncService;

    @Override
    public SimpleTimerCreateResponseDto createSimpleTimer(SimpleTimerCreateRequestDto requestDto) {
        Member member = timerSyncService.lockMember(UserContextHolder.getUserId());
        TimerSyncService.validateCreateMetadata(requestDto.getCreateRequestId(), requestDto.getEditedAt());

        boolean synced = requestDto.getCreateRequestId() != null;
        if (synced) {
            Optional<TimerCreateResult> receipt = timerSyncService.findCreation(member.getMemberId(),
                    requestDto.getCreateRequestId(), TimerType.SIMPLE);
            if (receipt.isPresent()) {
                TimerCreateResult result = receipt.get();
                return new SimpleTimerCreateResponseDto(result.timerId(), result.updatedAt());
            }
        } else {
            checkSimpleTimerCountLimit(member);
        }

        SimpleTimer savedTimer = simpleTimerRepository.save(SimpleTimer.of(member, requestDto.getTime(), requestDto.getEditedAt()));
        SimpleTimerCreateResponseDto response = SimpleTimerCreateResponseDto.from(savedTimer);
        if (synced) {
            timerSyncService.recordCreation(member, requestDto.getCreateRequestId(), TimerType.SIMPLE,
                    savedTimer.getId(), response.getUpdatedAt());
            pruneSimpleTimers(member);
        }
        return response;
    }

    @Override
    public TimerMutationResponse deleteSimpleTimer(Long simpleTimerId, TimerDeleteRequest dto) {
        Member member = timerSyncService.lockMember(UserContextHolder.getUserId());
        UUID editId = dto == null ? null : dto.editId();
        Long editedAt = dto == null ? null : dto.editedAt();
        boolean synced = TimerSyncService.isSyncEdit(editId, editedAt);
        if (synced) {
            Optional<TimerMutationResponse> receipt = timerSyncService.findEdit(member.getMemberId(), TimerType.SIMPLE,
                    simpleTimerId, editId, TimerEditOperation.DELETE);
            if (receipt.isPresent()) {
                return receipt.get();
            }
        }

        SimpleTimer timer = findOwnedTimer(simpleTimerId, member, synced);
        // 삭제는 기기 시각과 관계없이 이기며, 이미 삭제된 타이머의 시각을 보존한다.
        if (timer.getSimpleTimerDeletedAt() == null) {
            timer.deleteSimpleTimer(editedAt);
        }
        if (!synced) {
            return null;
        }
        timerSyncService.recordEdit(member, TimerType.SIMPLE, simpleTimerId, editId,
                TimerEditOperation.DELETE, timer.getSimpleTimerUpdatedAt());
        return new TimerMutationResponse(timer.getSimpleTimerUpdatedAt());
    }

    @Override
    public TimerMutationResponse updateSimpleTimer(Long simpleTimerId, SimpleTimerUpdateRequestDto dto) {
        Member member = timerSyncService.lockMember(UserContextHolder.getUserId());
        boolean synced = TimerSyncService.isSyncEdit(dto.getEditId(), dto.getEditedAt());
        if (synced) {
            Optional<TimerMutationResponse> receipt = timerSyncService.findEdit(member.getMemberId(), TimerType.SIMPLE,
                    simpleTimerId, dto.getEditId(), TimerEditOperation.UPDATE);
            if (receipt.isPresent()) {
                return receipt.get();
            }
        }

        SimpleTimer timer = findOwnedTimer(simpleTimerId, member, synced);
        if (synced) {
            if (timer.getSimpleTimerDeletedAt() != null) {
                throw new CustomException(ErrorCode.TIMER_DELETED);
            }
            if (TimerSyncService.effectiveEditedAt(timer.getEditedAt(), timer.getSimpleTimerUpdatedAt()) > dto.getEditedAt()) {
                throw new TimerSyncConflictException(SimpleTimerRecord.from(timer));
            }
        }
        timer.updateSimpleTimer(dto.getTime(), dto.getEditedAt());
        if (!synced) {
            return null;
        }
        timerSyncService.recordEdit(member, TimerType.SIMPLE, simpleTimerId, dto.getEditId(),
                TimerEditOperation.UPDATE, timer.getSimpleTimerUpdatedAt());
        return new TimerMutationResponse(timer.getSimpleTimerUpdatedAt());
    }

    @Override
    public void createDefaultSimpleTimer(UUID memberId) {
        Member member = timerSyncService.lockMember(memberId);
        int currentCount = simpleTimerRepository.countByMemberAndSimpleTimerDeletedAtNull(member);
        int maxCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT);
        int availableCount = Math.max(0, maxCount - currentCount);
        int initialCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT);
        String initialValues = policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES);

        List<SimpleTimer> timerList = new ArrayList<>();
        for (Integer time : getTimeList(Math.min(initialCount, availableCount), initialValues)) {
            timerList.add(SimpleTimer.of(member, time, null));
        }
        simpleTimerRepository.saveAll(timerList);
    }

    private SimpleTimer findOwnedTimer(Long timerId, Member member, boolean includeDeleted) {
        Optional<SimpleTimer> timer = includeDeleted
                ? simpleTimerRepository.findByIdAndMember(timerId, member)
                : simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(timerId, member);
        return timer.orElseThrow(() -> new CustomException(ErrorCode.SIMPLE_TIMER_NOT_FOUND));
    }

    private void checkSimpleTimerCountLimit(Member member) {
        int count = simpleTimerRepository.countByMemberAndSimpleTimerDeletedAtNull(member);
        int maxCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT);
        if (count >= maxCount) {
            throw new CustomException(ErrorCode.SIMPLE_TIMER_MAX_COUNT_VIOLATION);
        }
    }

    private void pruneSimpleTimers(Member member) {
        List<SimpleTimer> timerList = simpleTimerRepository.findByMemberAndSimpleTimerDeletedAtNullOrderBySimpleTimerTimeAsc(member);
        timerList.sort(Comparator.comparingLong((SimpleTimer timer) -> TimerSyncService.effectiveEditedAt(
                        timer.getEditedAt(), timer.getSimpleTimerUpdatedAt()))
                .thenComparing(SimpleTimer::getId).reversed());
        int maxCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT);
        for (int index = maxCount; index < timerList.size(); index++) {
            timerList.get(index).deleteSimpleTimer(null);
        }
    }

    private List<Integer> getTimeList(int count, String str) {
        String[] strArr = str.trim().split(",");
        List<Integer> integerList = new ArrayList<>();
        for (String value : strArr) {
            if (integerList.size() >= count) {
                break;
            }
            integerList.add(Integer.parseInt(value.trim()));
        }
        return integerList;
    }
}
