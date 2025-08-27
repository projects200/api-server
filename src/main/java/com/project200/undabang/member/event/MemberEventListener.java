package com.project200.undabang.member.event;

import com.project200.undabang.member.dto.event.MemberSignedUpEvent;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 관련 이벤트를 처리하는 리스너 클래스입니다.
 * 이 클래스는 회원 가입 이벤트를 수신하고, 해당 이벤트에 따라 기본 심플 타이머를 생성하는 작업을 수행합니다.
 * 이벤트는 트랜잭션이 성공적으로 커밋된 후 처리됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberEventListener {
    private final SimpleTimerCommandService simpleTimerCommandService;

    /**
     * 회원 가입 이벤트(MemberSignedUpEvent)를 처리하는 메서드입니다.
     * 해당 메서드는 회원 가입이 성공적으로 완료된 후 호출되며, 가입된 회원의 기본 심플 타이머를 생성합니다.
     */
    @Async("generalPurposeAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberSignedUp(MemberSignedUpEvent event) {
        try {
            if (event.memberId() == null) {
                log.error("회원 가입후 생성된 회원의 식별자가 올바르게 전달되지 않았습니다");
                return;
            }
            simpleTimerCommandService.createDefaultSimpleTimer(event.memberId());
        } catch (Exception e) {
            log.error("회원 가입 기본 심플 타이머 생성 중 오류 발생. Member ID : {}", event.memberId(), e);
        }
    }
}
