package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;

public interface MemberQueryService {
    MemberRegistrationStatusResponseDto getRegistrationStatus();
    MemberScoreResponseDto getMemberScore();
}
