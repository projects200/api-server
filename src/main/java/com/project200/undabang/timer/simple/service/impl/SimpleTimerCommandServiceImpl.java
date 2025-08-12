package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SimpleTimerCommandServiceImpl implements SimpleTimerCommandService {
    private final PolicyService policyService;
    private final SimpleTimerRepository simpleTimerRepository;

    /**
     * 기본 심플 타이머를 생성하는 메서드입니다.
     *
     * 정책 설정에 따라 초기 심플 타이머 개수와 초기 값들을 가져와 검증한 뒤,
     * 현재 사용자(Member)와 연관된 심플 타이머 객체를 생성합니다.
     * 생성된 객체는 저장소에 저장됩니다.
     *
     * 예외 상황:
     * - 정책 설정에서 심플 타이머 정보를 올바르게 가져오지 못할 경우
     * - 현재 사용자 정보 조회에 실패할 경우
     *
     * 오류 발생 시 {@link CustomException} 예외를 발생시킵니다.
     */
    @Override
    public void createDefaultSimpleTimer(Member member) {
        int simpleTimerCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT);
        String simpleTimerValue = policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES);

        List<Integer> timeList = getTimeList(simpleTimerValue);

        if (!verifyTimerSettings(simpleTimerCount, timeList)) {
            log.error("정책에 설정된 타이머 개수와 실제 값의 개수가 다릅니다.");
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        List<SimpleTimer> simpleTimerList = createSimpleTimerList(member, timeList);
        simpleTimerRepository.saveAll(simpleTimerList);
    }

    /**
     * 주어진 문자열을 구분자 ','로 분리하여 정수 목록으로 변환하여 반환합니다.
     *
     * @param str 쉼표로 구분된 정수 문자열
     * @return 문자열로부터 변환된 정수 리스트
     */
    private List<Integer> getTimeList(String str) {
        String[] strArr = str.trim().split(",");
        List<Integer> integerList = new ArrayList<>();

        for (String s : strArr) {
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

    /**
     * 주어진 타이머 수와 타이머 리스트의 크기를 검증합니다.
     *
     * @param simpleTimerCount 심플 타이머의 예상 개수
     * @param simpleTimerList  심플 타이머의 실제 설정값 리스트
     * @return 예상 개수와 실제 리스트 크기가 일치하면 true, 그렇지 않으면 false
     */
    private boolean verifyTimerSettings(int simpleTimerCount, List<Integer> simpleTimerList) {
        return simpleTimerCount == simpleTimerList.size();
    }
}
