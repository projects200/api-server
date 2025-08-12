package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.simple.dto.GetSimpleTimerResponseDto;
import com.project200.undabang.timer.simple.dto.SimpleTimerRecord;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import com.project200.undabang.timer.simple.service.SimpleTimerQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimpleTimerQueryServiceImpl implements SimpleTimerQueryService {
    private final SimpleTimerRepository simpleTimerRepository;
    private final MemberRepository memberRepository;

    @Override
    public GetSimpleTimerResponseDto getSimpleTimers() {
        UUID memberId = UserContextHolder.getUserId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<SimpleTimer> simpleTimerList = simpleTimerRepository.findByMemberAndSimpleTimerDeletedAtNull(member);

        return createSimpleTimerResponse(simpleTimerList);
    }

    private GetSimpleTimerResponseDto createSimpleTimerResponse(List<SimpleTimer> simpleTimerList) {
        List<SimpleTimerRecord> simpleTimerRecordList = simpleTimerList.stream()
                .map(SimpleTimerRecord::from)
                .toList();

        return GetSimpleTimerResponseDto.of(simpleTimerRecordList);
    }
}
