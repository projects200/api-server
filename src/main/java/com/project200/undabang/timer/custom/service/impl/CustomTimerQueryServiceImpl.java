package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.custom.dto.response.CustomTimerListResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerRecord;
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
     * 현재 사용자 ID를 기반으로 해당 사용자가 생성한 사용자 정의 타이머 목록을 반환합니다.
     * 목록에는 삭제되지 않은 타이머만 포함되며, 각 타이머는 CustomTimerRecord 형태로 매핑됩니다.
     */
    @Override
    public CustomTimerListResponse getCustomTimerList() {
        UUID memberId = UserContextHolder.getUserId();
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<CustomTimerRecord> recordList = findActiveTimersAndMapToRecords(member);

        return CustomTimerListResponse.from(recordList);
    }

    /**
     * 주어진 회원 정보를 기반으로 사용자 정의 타이머의 목록중 삭제되지 않은 타이머를
     * CustomTimerRecord 형태로 변환하여 반환합니다.
     */
    private List<CustomTimerRecord> findActiveTimersAndMapToRecords(Member member) {
        List<CustomTimer> customTimerList = customTimerRepository.findAllByMemberAndCustomTimerDeletedAtNull(member);

        return customTimerList.stream()
                .map(customTimer -> new CustomTimerRecord(customTimer.getId(), customTimer.getCustomTimerName()))
                .toList();
    }
}
