package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;

public interface MemberQueryService {
    MemberRegistrationStatusResponseDto getRegistrationStatus();
}
