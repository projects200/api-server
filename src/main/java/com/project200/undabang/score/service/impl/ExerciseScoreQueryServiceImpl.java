package com.project200.undabang.score.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.score.dto.response.EarnablePointsInfoResponseDto;
import com.project200.undabang.score.dto.response.ValidityWindowDto;
import com.project200.undabang.score.service.ExerciseScoreQueryService;
import com.project200.undabang.score.validation.ExercisePolicyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseScoreQueryServiceImpl implements ExerciseScoreQueryService {

    private final ExerciseRepository exerciseRepository;
    private final PolicyService policyService;
    private final MemberRepository memberRepository;
    private final ExercisePolicyValidator exercisePolicyValidator;

    /**
     * 운동 기록에 대한 획득 가능한 점수 정보를 조회합니다.
     *
     * @return EarnablePointsInfoResponseDto - 획득 가능한 점수 정보
     */
    @Override
    public EarnablePointsInfoResponseDto getEarnablePointsInfo() {
        UUID memberId = UserContextHolder.getUserId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 정책 값 조회
        byte pointsPerExercise = policyService.getPolicyValueAsByte(PolicyKey.POINTS_PER_EXERCISE);
        byte maxScore = policyService.getPolicyValueAsByte(PolicyKey.EXERCISE_SCORE_MAX_POINTS);
        int maxRecordsPerDay = policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_RECORD_MAX_PER_DAY);

        // 운동 기록 후 점수 획득 가능한 유효 기간 계산
        LocalDateTime endDateTime = LocalDateTime.now();
        LocalDateTime startDateTime = exercisePolicyValidator.calculateValidityEndDate();
        ValidityWindowDto validityWindow = new ValidityWindowDto(startDateTime, endDateTime);

        // 유효 기간 내에 점수를 획득할 수 있는 날짜 목록 계산
        List<LocalDate> earnableDates = calculateEarnableDates(member, startDateTime.toLocalDate(), endDateTime.toLocalDate(), maxRecordsPerDay);

        return new EarnablePointsInfoResponseDto(
                pointsPerExercise,
                member.getMemberScore(),
                maxScore,
                validityWindow,
                earnableDates
        );
    }

    /**
     * 유효 기간 내에 운동 기록을 남길 수 있는 날짜 목록을 계산합니다.
     * 하루 최대 운동 기록 수를 초과하지 않는 날짜만 포함됩니다.
     *
     * @param member           - 회원 정보
     * @param startDate        - 유효 기간 시작 날짜
     * @param endDate          - 유효 기간 종료 날짜
     * @param maxRecordsPerDay - 하루 최대 운동 기록 수
     * @return List<LocalDate> - 운동 기록이 가능한 날짜 목록
     */
    private List<LocalDate> calculateEarnableDates(Member member, LocalDate startDate, LocalDate endDate, int maxRecordsPerDay) {
        // 단 한 번의 쿼리로 기간 내 모든 날짜의 운동 횟수 정보를 가져옵니다.
        Map<LocalDate, Long> dailyExerciseCounts = exerciseRepository.findExerciseCountsByDateBetween(member, startDate, endDate);

        // 유효 기간 내의 모든 날짜 스트림을 생성합니다.
        return startDate.datesUntil(endDate.plusDays(1)) // endDate를 포함하기 위해 +1
                // 가져온 Map 데이터를 기반으로 점수 획득 가능 여부를 필터링합니다.
                // getOrDefault: 운동 기록이 없는 날짜는 count가 0L 이므로 점수 획득이 가능합니다.
                .filter(date -> dailyExerciseCounts.getOrDefault(date, 0L) < maxRecordsPerDay)
                .toList();
    }

}
