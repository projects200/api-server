package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.dto.record.MemberProfileRecord;
import com.project200.undabang.member.dto.response.*;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberQueryService;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {

    private final MemberRepository memberRepository;
    private final ExerciseRepository exerciseRepository;

    private final PolicyService policyService;

    private static final int RECENT_EXERCISE_PERIOD_DAYS = 30; // 최근 운동기간

    /**
     * 특정 회원의 운동 기록을 지정된 기간 동안 조회합니다.
     * 조회 대상 회원은 삭제된 상태가 아니어야 하며, 기간이 유효하지 않을 경우 예외가 발생합니다.
     * 기한은 광복부터 오늘 이내여야 하며, 시작날짜는 끝 날짜를 넘을 수 없습니다.
     */
    @Override
    public List<FindExerciseRecordByPeriodResponseDto> getOtherMemberCalendars(UUID memberId, LocalDate startDate, LocalDate endDate) {
        validateNotSelfRequest(memberId);

        Member otherMember = memberRepository.findMemberProfileByMemberIdAndMemberDeletedAtNull(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // TODO : 추후 차단 기능 개발 시, 다른 회원이 차단한 경우 검색 안되게 하는 기능 추가

        if (startDate.isBefore(LocalDate.of(1945, 8, 15)) || endDate.isAfter(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.IMPOSSIBLE_INPUT_DATE);
        }

        return exerciseRepository.findExercisesByPeriod(otherMember.getMemberId(), startDate, endDate);
    }

    /**
     * 다른 회원의 프로필 정보를 조회합니다.
     * 조회 대상 회원은 삭제되지 않은 상태여야 하며, 회원이 존재하지 않을 경우 예외가 발생합니다.
     * 회원의 연간 운동 횟수와 최근 특정 기간 동안의 운동 횟수를 함께 반환합니다.
     *
     * @param memberId 조회할 회원의 고유 식별자(UUID)
     * @return GetOtherMemberProfileResponse 객체로, 다른 회원의 프로필 정보,
     *         연간 운동 횟수, 최근 운동 횟수를 포함합니다.
     */
    @Override
    public GetOtherMemberProfileResponse getOtherMemberProfile(UUID memberId) {
        validateNotSelfRequest(memberId);
        // TODO : 추후 차단 기능 개발 시, 다른 회원이 차단한 경우 검색 안되게 하는 기능 추가

        MemberProfileRecord record = memberRepository.findMemberProfileWithPreferredExerciseActiveByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        int yearlyExerciseCounts = memberRepository.countMemberExerciseInThisYear(record.memberId()).intValue();
        int exerciseCountInLastDays = memberRepository.countMemberExerciseInLastDays(record.memberId(), RECENT_EXERCISE_PERIOD_DAYS).intValue();

        return GetOtherMemberProfileResponse.from(record, yearlyExerciseCounts, exerciseCountInLastDays);
    }

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
     * 현재 사용자의 멤버 프로필 정보를 조회합니다.
     * 조회된 프로필 정보에는 사용자의 선호 운동 상태, 연간 운동 횟수,
     * 최근 특정 기간 동안의 운동 횟수가 포함됩니다.
     *
     * @return MemberProfileResponse 객체로, 현재 사용자의 멤버 프로필 정보,
     * 연간 운동 횟수 및 최근 운동 횟수를 포함합니다.
     */
    @Override
    public MemberProfileResponse getMemberProfile() {
        MemberProfileRecord memberProfileRecord = memberRepository.findMemberProfileWithPreferredExerciseActiveByMemberId(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        int yearlyExerciseCounts = memberRepository.countMemberExerciseInThisYear(memberProfileRecord.memberId()).intValue();

        int exerciseCountInLastDays = memberRepository.countMemberExerciseInLastDays(memberProfileRecord.memberId(), RECENT_EXERCISE_PERIOD_DAYS).intValue();

        return MemberProfileResponse.from(memberProfileRecord, yearlyExerciseCounts, exerciseCountInLastDays);
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

    /**
     * 사용자가 본인에게 요청을 보내는 것을 방지하기 위한 검증 메서드입니다.
     * 요청한 사용자의 ID가 확인 대상 ID와 동일할 경우 예외를 발생시킵니다.
     */
    private void validateNotSelfRequest(UUID memberId) {
        UUID currentUserId = UserContextHolder.getUserId();

        if (memberId.equals(currentUserId)) {
            throw new CustomException(ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
        }
    }
}
