package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {

    private final MemberRepository memberRepository;

    @Override
    public MemberRegistrationStatusResponseDto getRegistrationStatus() {
        UUID userId = UserContextHolder.getUserId();

        boolean isRegistered = memberRepository.existsByMemberId(userId);

        return MemberRegistrationStatusResponseDto.builder()
                .memberId(userId)
                .isRegistered(isRegistered)
                .build();

    }
}
