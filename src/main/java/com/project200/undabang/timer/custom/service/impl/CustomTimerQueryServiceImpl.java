package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.custom.dto.response.CustomTimerDetailResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerListResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerRecord;
import com.project200.undabang.timer.custom.dto.response.CustomTimerStepRecord;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
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
    private final CustomTimerStepRepository customTimerStepRepository;
    private final MemberRepository memberRepository;

    /**
     * 지정된 사용자 정의 타이머 ID를 기반으로 타이머의 세부 정보를 조회하고 반환합니다.
     * 사용자와 타이머 소유자가 일치하지 않을 경우 권한 오류를 발생시킵니다.
     */
    @Override
    public CustomTimerDetailResponse getCustomTimerDetail(Long customTimerId) {
        Member member = getMember();
        CustomTimer timer = customTimerRepository.findById(customTimerId)
                .orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_TIMER_NOT_FOUND));

        // 회원이 다른 사람의 커스텀 리스트를 조회하려 하는 경우 예외 발생
        if (!timer.getMember().equals(member)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }

        // 자신의 커스텀 리스트와 스텝 반환
        return findCustomTimerStepListAndMapToResponse(timer);
    }

    /**
     * 현재 사용자 ID를 기반으로 해당 사용자가 생성한 사용자 정의 타이머 목록을 반환합니다.
     * 목록에는 삭제되지 않은 타이머만 포함되며, 각 타이머는 CustomTimerRecord 형태로 매핑됩니다.
     */
    @Override
    public CustomTimerListResponse getCustomTimerList() {
        Member member = getMember();

        List<CustomTimerRecord> recordList = findActiveTimersAndMapToRecords(member);

        return CustomTimerListResponse.from(recordList);
    }

    /**
     * 주어진 CustomTimer 객체를 기반으로 사용자 정의 타이머 단계 목록을 조회하고,
     * 이를 매핑하여 CustomTimerDetailResponse 객체로 반환합니다.
     */
    private CustomTimerDetailResponse findCustomTimerStepListAndMapToResponse(CustomTimer customTimer) {
        List<CustomTimerStepRecord> customTimerStepRecordList = customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(customTimer)
                .stream().map(step -> new CustomTimerStepRecord(step.getId(), step.getCustomTimerStepName(), step.getCustomTimerStepOrder(), step.getCustomTimerStepTime()))
                .toList();

        return CustomTimerDetailResponse.from(customTimer, customTimerStepRecordList);
    }

    /**
     * 현재 컨텍스트의 사용자 ID를 기반으로 데이터베이스에서 회원 정보를 조회합니다.
     * 해당 ID로 회원이 존재하지 않을 경우 CustomException을 발생시킵니다.
     */
    private Member getMember() {
        UUID memberId = UserContextHolder.getUserId();
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
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
