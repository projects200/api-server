package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.MemberProfileResponse;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;

public interface MemberQueryService {
    MemberRegistrationStatusResponseDto getRegistrationStatus();
    MemberScoreResponseDto getMemberScore();

    /**
     * 현재 회원의 프로필 정보를 조회합니다.
     *
     * @return 회원 프로필 정보를 담고 있는 MemberProfileResponse 객체
     */
    MemberProfileResponse getMemberProfile();
}
