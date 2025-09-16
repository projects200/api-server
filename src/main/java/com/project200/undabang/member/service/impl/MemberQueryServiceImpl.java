package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.response.CheckNicknameDuplicateResponse;
import com.project200.undabang.member.dto.response.MemberProfileResponse;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberQueryService;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {

    private final MemberRepository memberRepository;

    private final PolicyService policyService;

    private static final int RECENT_EXERCISE_PERIOD_DAYS = 30; // 최근 운동기간

    /**
     * 현재 사용자가 시스템에 등록되어 있는지의 여부를 확인합니다.
     *
     * @return MemberRegistrationStatusResponseDto 객체로, 사용자의 회원 ID와 등록 여부를 포함합니다.
     */
    @Override
    public MemberRegistrationStatusResponseDto getRegistrationStatus() {
        UUID userId = UserContextHolder.getUserId();

        boolean isRegistered = memberRepository.existsByMemberId(userId);

        return MemberRegistrationStatusResponseDto.builder()
                .memberId(userId)
                .isRegistered(isRegistered)
                .build();

    }

    /**
     * 현재 사용자의 멤버 점수를 조회합니다.
     * 점수는 정책에서 정의된 최대 점수 및 최소 점수와 함께 반환됩니다.
     *
     * @return MemberScoreResponseDto 객체로, 멤버 ID, 현재 점수, 정책 기준 최대 점수, 정책 기준 최소 점수를 포함합니다.
     */
    @Override
    public MemberScoreResponseDto getMemberScore() {
        Member member = findMemberById();
        int maxScore = policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_SCORE_MAX_POINTS);
        int minScore = policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_SCORE_MIN_POINTS);

        return MemberScoreResponseDto.builder()
                .memberId(member.getMemberId())
                .memberScore(member.getMemberScore())
                .policyMaxScore(maxScore)
                .policyMinScore(minScore)
                .build();
    }

    /**
     * 현재 사용자와 관련된 멤버 프로필 정보를 조회합니다.
     * 삭제된 멤버는 조회 대상에서 제외되며, 해당 사용자의 프로필 정보가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @return MemberProfileResponse 객체로, 현재 사용자와 연결된 멤버의 프로필 정보를 포함합니다.
     */
    @Override
    public MemberProfileResponse getMemberProfile() {
        Member member = memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        int yearlyExerciseCounts = memberRepository.countMemberExerciseInThisYear(member.getMemberId()).intValue();

        int exerciseCountInLastDays = memberRepository.countMemberExerciseInLastDays(member.getMemberId(), RECENT_EXERCISE_PERIOD_DAYS).intValue();

        return MemberProfileResponse.of(member, yearlyExerciseCounts, exerciseCountInLastDays);
    }

    /**
     * 주어진 닉네임이 시스템에서 이미 사용 중인지 확인합니다.
     */
    @Override
    public CheckNicknameDuplicateResponse checkDuplicateNickname(String nickname) {

        return CheckNicknameDuplicateResponse.of(!memberRepository.existsByMemberNickname(nickname));
    }


    /**
     * 주어진 사용자 ID를 기반으로 멤버 정보를 조회합니다.
     * 삭제된 멤버는 조회 대상에서 제외되며, 조회 결과가 없을 경우 예외가 발생합니다.
     */
    private Member findMemberById() {
        return memberRepository.findByMemberIdAndMemberDeletedAtNull(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
