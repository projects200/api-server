package com.project200.undabang.member.service;

import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.member.dto.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MemberQueryService {
    MemberRegistrationStatusResponseDto getRegistrationStatus();
    MemberScoreResponseDto getMemberScore();

    /**
     * 현재 회원의 프로필 정보를 조회합니다.
     *
     * @return 회원 프로필 정보를 담고 있는 MemberProfileResponse 객체
     */
    MemberProfileResponse getMemberProfile();

    CheckNicknameDuplicateResponse checkDuplicateNickname(String nickname);

    GetOtherMemberProfileResponse getOtherMemberProfile(UUID memberId);

    List<FindExerciseRecordByPeriodResponseDto> getOtherMemberCalendars(UUID memberId, LocalDate startDate, LocalDate endDate);
}
