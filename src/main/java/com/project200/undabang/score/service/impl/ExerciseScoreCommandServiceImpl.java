package com.project200.undabang.score.service.impl;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.ErrorLevel;
import com.project200.undabang.admin.entity.dto.MemberScoreErrorDto;
import com.project200.undabang.admin.util.ErrorLogsUtils;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.score.dto.internal.EarnablePointsInfoDto;
import com.project200.undabang.score.service.ExerciseScoreCommandService;
import com.project200.undabang.score.validation.ExercisePolicyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseScoreCommandServiceImpl implements ExerciseScoreCommandService {

    private final ExerciseRepository exerciseRepository;
    private final PolicyService policyService;
    private final ExercisePolicyValidator exercisePolicyValidator;
    private final NotifyErrorToAdmin notifyErrorToAdmin;

    @Value("${spring.profiles.active}")
    private String profile;

    /**
     * 운동 기록에 대한 점수 부여를 처리합니다.
     * Propagation.REQUIRES_NEW를 사용하여 이 메소드의 트랜잭션이 실패하더라도,
     * 이 메소드를 호출한 부모 트랜잭션(운동 기록 생성)에 영향을 주지 않습니다.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public byte awardPointsForExercise(Exercise exercise) {
        byte actualEarnedPoints = 0; // 실제 획득 점수를 담을 변수

        try {
            EarnablePointsInfoDto pointsInfo = checkEarnablePoints(exercise.getMember(), exercise.getExerciseStartedAt());

            if(true){
                throw new RuntimeException("고의로 테스트 발생");
            }

            if (pointsInfo.isEarnable()) {
                Member member = exercise.getMember();
                byte minScore = policyService.getPolicyValueAsByte(PolicyKey.EXERCISE_SCORE_MIN_POINTS);
                byte maxScore = policyService.getPolicyValueAsByte(PolicyKey.EXERCISE_SCORE_MAX_POINTS);

                // member.addScore()가 반환하는 실제 증가분을 받음
                actualEarnedPoints = member.addScore(pointsInfo.getPointsToAward(), minScore, maxScore);
            }
            return actualEarnedPoints;
        } catch (Exception e) {
            log.error("운동 기록 점수 부여 중 오류 발생. exerciseId: {}, memberId: {}",
                    exercise.getId(), exercise.getMember().getMemberId(), e);

            // 슬랙 알림을 통해 개발자에게 비동기로 공지
            MemberScoreErrorDto dto = createMemberScoreErrorDto(e);
            notifyErrorToAdmin.sendMemberScoreIncreaseErrorToSlack(dto);

            return 0; // 예외 발생 시 0점 반환
        }
    }

    private MemberScoreErrorDto createMemberScoreErrorDto(Exception e){
        UUID memberId = UserContextHolder.getUserId();

        // 현재 쓰레드의 Http 요청 컨텍스트에 접근할 수 있도록 RequestContextHolder 사용
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String requestUri = attributes.getRequest().getRequestURI();
        String requestMethod = attributes.getRequest().getMethod();

        String errorClassName = ErrorLogsUtils.findClassErrorHappened(e);
        String stackTrace = ErrorLogsUtils.getStructuredStackTrace(e);
        String actionGuide = ErrorLogsUtils.createActionGuide(e);

        return MemberScoreErrorDto.builder()
                .httpMethod(requestMethod)
                .requestUri(requestUri)
                .userIdentifier(memberId)
                .serviceName("ExerciseScoreCommandServiceImpl")
                .className(errorClassName)
                .errorLevel(ErrorLevel.ERROR)
                .summary("운동기록 생성시 점수가 추가되지 않았습니다.")
                .errorOccurredAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .environment(profile)
                .stackTrace(stackTrace)
                .actionGuide(actionGuide)
                .build();
    }


    /**
     * 획득 가능한 점수를 계산하는 private 헬퍼 메소드입니다.
     * 사용자가 운동 기록을 남길 때, 해당 운동 기록에 대해 점수를 획득할 수 있는지 검증합니다.
     * 이 메소드는 점수 획득 조건을 확인하고, 조건을 만족할 경우 획득 가능한 점수를 반환합니다.
     *
     * @param member            점수를 획득하려는 사용자
     * @param exerciseStartedAt 운동 시작 시간
     * @return 점수를 획득할 수 있는지 여부와 획득 가능한 점수
     */
    private EarnablePointsInfoDto checkEarnablePoints(Member member, LocalDateTime exerciseStartedAt) {
        // 1. 점수 획득 유효 기간 검증
        if (isOutsideValidityPeriod(exerciseStartedAt)) {
            log.debug("점수 부여 조건 확인: 유효 기간 벗어남. memberId: {}", member.getMemberId());
            return EarnablePointsInfoDto.notEarnable();
        }

        // 2. 일일 최대 기록 횟수 검증
        int maxRecordsPerDay = policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_RECORD_MAX_PER_DAY);
        long recordsOnDate = exerciseRepository.countByMemberAndExerciseStartedAt(
                member,
                exerciseStartedAt.toLocalDate()
        );

        if (recordsOnDate >= maxRecordsPerDay) {
            log.debug("점수 부여 조건 확인: 일일 최대 기록 횟수 초과. memberId: {}, recordsOnDate: {}", member.getMemberId(), recordsOnDate);
            return EarnablePointsInfoDto.notEarnable();
        }

        // 3. 점수 획득 조건 충족
        byte pointsPerExercise = policyService.getPolicyValueAsByte(PolicyKey.POINTS_PER_EXERCISE);
        return EarnablePointsInfoDto.earnable(pointsPerExercise);
    }

    /**
     * 운동 기록이 유효 기간 내에 있는지 확인합니다.
     * 유효 기간은 정책에 정의된 값에 따라 다릅니다.
     *
     * @param exerciseStartedAt 운동 시작 시간
     * @return 유효 기간을 벗어났다면 true, 그렇지 않다면 false
     */
    private boolean isOutsideValidityPeriod(LocalDateTime exerciseStartedAt) {
        LocalDateTime endDate = exercisePolicyValidator.calculateValidityEndDate();

        return exerciseStartedAt.isBefore(endDate);
    }
}

