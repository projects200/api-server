package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.custom.dto.response.CustomTimerRecord;
import com.project200.undabang.timer.custom.dto.response.GetCustomTimerListResponse;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.service.CustomTimerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomTimerQueryServiceImpl implements CustomTimerQueryService {
    private final CustomTimerRepository customTimerRepository;
    private final MemberRepository memberRepository;

    /**
     * 현재 회원 ID를 기반으로 삭제되지 않은 사용자 정의 타이머의 목록을 반환합니다.
     */
    @Override
    public GetCustomTimerListResponse getCustomTimerList() {
        UUID memberId = UserContextHolder.getUserId();
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<CustomTimerRecord> recordList = createCustomTimerList(member);

        return createCustomTimerListResponse(recordList);
    }

    /**
     * 주어진 사용자 정의 타이머 기록 목록을 기반으로 사용자 정의 타이머 목록 응답 객체를 생성합니다.
     */
    private GetCustomTimerListResponse createCustomTimerListResponse(List<CustomTimerRecord> recordList) {
        return GetCustomTimerListResponse.builder()
                .customTimerCount(recordList.size())
                .customTimers(recordList)
                .build();
    }

    /**
     * 지정된 회원의 삭제되지 않은 사용자 정의 타이머 목록을 생성합니다.
     */
    private List<CustomTimerRecord> createCustomTimerList(Member member) {
        List<CustomTimer> findCustomTimerList = customTimerRepository.findByMemberAndCustomTimerDeletedAtNull(member);

        return findCustomTimerList.stream()
                .map(customTimer -> CustomTimerRecord.of(customTimer.getId(), customTimer.getCustomTimerName()))
                .toList();
    }
}
