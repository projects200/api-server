package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomTimerCommandServiceImpl implements CustomTimerCommandService {
    private final CustomTimerRepository customTimerRepository;
    private final CustomTimerStepRepository customTimerStepRepository;
    private final MemberRepository memberRepository;

    @Override
    public CustomTimerCreateResponse createCustomTimer(CustomTimerCreateRequest dto) {
        Member member = getMember(UserContextHolder.getUserId());


        return null;
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
