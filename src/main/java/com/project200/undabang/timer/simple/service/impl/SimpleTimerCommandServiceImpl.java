package com.project200.undabang.timer.simple.service.impl;

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
     * 주어진 회원(Member)에 대해 초기화된 기본 심플 타이머(SimpleTimer)를 생성하는 메서드입니다.
     * 정책 서비스에서 초기 개수와 초기 값을 가져와 이를 기반으로 심플 타이머 객체를 생성하고 저장소에 저장합니다.
     *
     * @param member 초기화된 심플 타이머와 연관된 회원 객체
     */
    @Override
    public void createDefaultSimpleTimer(Member member) {
        int simpleTimerCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT);
        String simpleTimerValue = policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES);

        // 캐시에서 정책을 가져와서 리스트로 가공
        List<Integer> timeList = getTimeList(simpleTimerCount, simpleTimerValue);

        List<SimpleTimer> simpleTimerList = createSimpleTimerList(member, timeList);
        simpleTimerRepository.saveAll(simpleTimerList);
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
