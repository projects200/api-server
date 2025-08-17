package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerCreateRequestDto;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerCreateResponseDto;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SimpleTimerCommandServiceImpl implements SimpleTimerCommandService {
    private final PolicyService policyService;
    private final SimpleTimerRepository simpleTimerRepository;
    private final MemberRepository memberRepository;

    private static final int MAX_SIMPLE_TIMER_COUNT = 6;


    /**
     * 심플 타이머를 생성하는 메서드입니다.
     * 요청 데이터를 기반으로 새로운 심플 타이머를 생성하고 저장합니다.
     * 회원별로 생성 가능한 최대 심플 타이머 개수를 초과하면 예외를 발생시킵니다.
     */
    @Override
    public SimpleTimerCreateResponseDto createSimpleTimer(SimpleTimerCreateRequestDto requestDto) {
        Member member = memberRepository.findById(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        int count = simpleTimerRepository.countDistinctByMemberAndSimpleTimerDeletedAtNull(member);

        if (count >= MAX_SIMPLE_TIMER_COUNT) {
            throw new CustomException(ErrorCode.SIMPLE_TIMER_MAX_COUNT_VIOLATION);
        }

        SimpleTimer timer = SimpleTimer.of(member, requestDto.getTime());
        SimpleTimer savedTimer = simpleTimerRepository.save(timer);

        return SimpleTimerCreateResponseDto.builder()
                .simpleTimerId(savedTimer.getId())
                .build();
    }

    /**
     * 주어진 심플 타이머 ID를 기반으로 심플 타이머를 삭제하는 메서드입니다.
     */
    @Override
    public void deleteSimpleTimer(Long simpleTimerId) {
        Member member = memberRepository.findById(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        SimpleTimer timer = simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(simpleTimerId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.SIMPLE_TIMER_NOT_EXIST));

        timer.deleteSimpleTimer();
    }

    /**
     * 지정된 심플 타이머 ID와 요청 데이터를 기반으로 심플 타이머를 업데이트하는 메서드입니다.
     */
    @Override
    public void updateSimpleTimer(Long simpleTimerId, SimpleTimerUpdateRequestDto dto) {
        UUID memberId = UserContextHolder.getUserId();
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        SimpleTimer timer = simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(simpleTimerId, member).orElseThrow(
                () -> new CustomException(ErrorCode.SIMPLE_TIMER_NOT_EXIST));

        timer.updateSimpleTimer(dto.getTime());
    }

    /**
     * 주어진 회원 ID를 기반으로 기본 심플 타이머를 생성하는 메서드입니다.
     */
    @Override
    public void createDefaultSimpleTimer(UUID memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        int simpleTimerCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT);
        String simpleTimerValue = policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES);

        // 캐시에서 정책을 가져와서 리스트로 가공
        List<Integer> timeList = getTimeList(simpleTimerCount, simpleTimerValue);

        List<SimpleTimer> simpleTimerList = createSimpleTimerList(member, timeList);
        simpleTimerRepository.saveAll(simpleTimerList);
    }

    /**
     * 주어진 문자열을 파싱하여 정해진 개수만큼의 정수를 리스트로 반환하는 메서드입니다.
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
     */
    private List<SimpleTimer> createSimpleTimerList(Member member, List<Integer> timeList) {
        List<SimpleTimer> simpleTimerList = new ArrayList<>();

        for (Integer time : timeList) {
            simpleTimerList.add(SimpleTimer.of(member, time));
        }

        return simpleTimerList;
    }
}
