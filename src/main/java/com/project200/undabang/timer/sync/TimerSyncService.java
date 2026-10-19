package com.project200.undabang.timer.sync;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.sync.entity.TimerCreateReceipt;
import com.project200.undabang.timer.sync.entity.TimerEditReceipt;
import com.project200.undabang.timer.sync.repository.TimerCreateReceiptRepository;
import com.project200.undabang.timer.sync.repository.TimerEditReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class TimerSyncService {
    private final MemberRepository memberRepository;
    private final TimerCreateReceiptRepository timerCreateReceiptRepository;
    private final TimerEditReceiptRepository timerEditReceiptRepository;

    /**
     * 명령 트랜잭션이 끝날 때까지 두 타이머 종류의 모든 쓰기를 회원 단위로 직렬화합니다.
     * 호출자는 타이머, 영수증, 개수를 조회하기 전에 이 잠금을 얻어야 합니다.
     */
    public Member lockMember(UUID memberId) {
        return memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Optional<TimerCreateResult> findCreation(UUID memberId, UUID createRequestId, TimerType timerType) {
        if (createRequestId == null) {
            return Optional.empty();
        }
        return timerCreateReceiptRepository.findByMember_MemberIdAndCreateRequestId(memberId, createRequestId)
                .map(receipt -> {
                    if (receipt.getTimerType() != timerType) {
                        throw new CustomException(ErrorCode.TIMER_REQUEST_ID_REUSED);
                    }
                    return new TimerCreateResult(receipt.getTimerId(), receipt.getUpdatedAt());
                });
    }

    public void recordCreation(Member member, UUID createRequestId, TimerType timerType,
                               Long timerId, LocalDateTime updatedAt) {
        timerCreateReceiptRepository.save(TimerCreateReceipt.of(member, createRequestId, timerType, timerId, updatedAt));
    }

    public Optional<TimerMutationResponse> findEdit(UUID memberId, TimerType timerType, Long timerId,
                                                   UUID editId, TimerEditOperation operation) {
        if (editId == null) {
            return Optional.empty();
        }
        return timerEditReceiptRepository.findByMember_MemberIdAndTimerTypeAndTimerIdAndEditId(
                        memberId, timerType, timerId, editId)
                .map(receipt -> {
                    if (receipt.getOperation() != operation) {
                        throw new CustomException(ErrorCode.TIMER_REQUEST_ID_REUSED);
                    }
                    return new TimerMutationResponse(receipt.getUpdatedAt());
                });
    }

    public void recordEdit(Member member, TimerType timerType, Long timerId, UUID editId,
                           TimerEditOperation operation, LocalDateTime updatedAt) {
        timerEditReceiptRepository.save(TimerEditReceipt.of(member, timerType, timerId, editId, operation, updatedAt));
    }

    public static void validateCreateMetadata(UUID createRequestId, Long editedAt) {
        if ((editedAt != null && createRequestId == null) || (editedAt != null && editedAt < 0)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public static boolean isSyncEdit(UUID editId, Long editedAt) {
        if ((editId == null) != (editedAt == null) || (editedAt != null && editedAt < 0)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return editId != null;
    }

    public static LocalDateTime currentTimestamp() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public static long effectiveEditedAt(Long editedAt, LocalDateTime serverUpdatedAt) {
        if (editedAt != null) {
            return editedAt;
        }
        // 기존 행의 서버 시간을 기존 JVM 시간대로 해석하며 기기 시간을 추측하지 않습니다.
        return serverUpdatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
