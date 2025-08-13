package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.event.MemberSignedUpEvent;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleTimerCommandServiceImpl implements SimpleTimerCommandService {
    private final PolicyService policyService;
    private final SimpleTimerRepository simpleTimerRepository;
    private final MemberRepository memberRepository;

    /**
     * 회원 가입 이벤트 발생 시 기본 심플 타이머를 생성하는 메서드입니다.
     * 이 메서드는 비동기적으로 실행되며, 트랜잭션 커밋 이후 이벤트 리스너로 동작합니다.
     *
     * @param event 회원 가입 이벤트 정보를 담고 있는 데이터 전송 객체
     */
    @Override
    @Async(value = "generalPurposeAsyncExecutor")
    // @TransactionalEventListener가 붙은 메소드에 동시에 기본 @Transactional 어노테이션이 붙으면 안됨
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void createDefaultSimpleTimer(MemberSignedUpEvent event) {
        Member member = memberRepository.findById(event.memberId()).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        try {
            int simpleTimerCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT);
            String simpleTimerValue = policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES);

            // 캐시에서 정책을 가져와서 리스트로 가공
            List<Integer> timeList = getTimeList(simpleTimerCount, simpleTimerValue);

            List<SimpleTimer> simpleTimerList = createSimpleTimerList(member, timeList);
            simpleTimerRepository.saveAll(simpleTimerList);
        } catch (Exception e) {
            log.error("회원 가입 기본 심플 타이머 생성 중 오류 발생. Member ID : {}", event.memberId(), e);
        }

    }

    /**
     * 주어진 문자열에서 시간을 나타내는 정수 리스트를 생성하여 반환하는 메서드입니다.
     * 문자열은 쉼표(,)로 구분된 값을 포함해야 하며, 최대 개수는 count로 제한됩니다.
     *
     * @param count 반환할 정수 리스트의 최대 개수
     * @param str 쉼표로 구분된 숫자 문자열
     * @return 파싱된 정수 리스트
     * @throws NumberFormatException 문자열이 올바른 정수 형태가 아닌 경우 발생
     */
    private List<Integer> getTimeList(int count, String str) {
        String[] strArr = str.trim().split(",");
        List<Integer> integerList = new ArrayList<>();

        for (String s : strArr) {
            if (integerList.size() >= count) {
                break;
            }
            integerList.add(Integer.parseInt(s.trim()));
        }

        return integerList;
    }

    /**
     * 회원과 시간 리스트를 기반으로 심플 타이머 객체 리스트를 생성합니다.
     *
     * @param member 심플 타이머와 연관된 회원 객체
     * @param timeList 생성할 심플 타이머의 시간 값 목록
     * @return 생성된 심플 타이머 객체 리스트
     */
    private List<SimpleTimer> createSimpleTimerList(Member member, List<Integer> timeList) {
        List<SimpleTimer> simpleTimerList = new ArrayList<>();

        for (Integer time : timeList) {
            simpleTimerList.add(SimpleTimer.of(member, time));
        }

        return simpleTimerList;
    }
}
