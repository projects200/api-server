package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.request.CreateExerciseLocationRequest;
import com.project200.undabang.member.dto.request.UpdateExerciseLocationRequest;
import com.project200.undabang.member.dto.response.CreateExerciseLocationResponse;
import com.project200.undabang.member.dto.response.UpdateExerciseLocationResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.ExerciseLocationCommandService;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseLocationCommandServiceImpl implements ExerciseLocationCommandService {
    private final ExerciseLocationRepository exerciseLocationRepository;
    private final MemberRepository memberRepository;
    private final PolicyService policyService;

    /**
     * 새로운 운동 위치를 생성하고 저장소에 저장한 후, 생성된 운동 위치의 식별자를 반환합니다.
     */
    @Transactional
    @Override
    public CreateExerciseLocationResponse createExerciseLocation(CreateExerciseLocationRequest request) {
        Member member = getMember(UserContextHolder.getUserId());

        validateForCreation(member, request.getName());

        ExerciseLocation exerciseLocation = request.toEntity(member);
        ExerciseLocation savedExerciseLocation = exerciseLocationRepository.save(exerciseLocation);

        return CreateExerciseLocationResponse.from(savedExerciseLocation);
    }

    /**
     * 전달받은 운동 위치 ID와 회원 식별자를 이용하여 운동 위치 정보를 업데이트하는 메서드.
     * 해당 회원이 소유한 운동 위치가 존재하지 않거나 삭제된 경우 예외를 발생시킵니다.
     */
    @Transactional
    @Override
    public UpdateExerciseLocationResponse updateExerciseLocation(Long locationId, UpdateExerciseLocationRequest request) {
        Member member = getMember(UserContextHolder.getUserId());

        ExerciseLocation exerciseLocation = exerciseLocationRepository.findByMemberAndExerciseLocationIdAndExerciseLocationDeletedAtNull(member, locationId).orElseThrow(
                () -> new CustomException(ErrorCode.EXERCISE_LOCATION_NOT_FOUND)
        );

        exerciseLocation.updateExerciseLocationName(request.getExerciseLocationName());

        return UpdateExerciseLocationResponse.from(exerciseLocation);
    }

    /**
     * 새로운 운동 위치 생성 시 유효성을 검증하는 메서드.
     * 운동 위치 이름의 중복 여부와 회원이 생성할 수 있는 운동 위치의 최대 개수를 검사하여
     * 규칙을 위반할 경우 예외를 발생시킴.
     */
    private void validateForCreation(Member member, String exerciseLocationName) {
        if (checkDuplicateExerciseLocationName(member, exerciseLocationName)) {
            throw new CustomException(ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED);
        }

        int maxCount = policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_LOCATION_MAX_COUNT);
        if (countMemberExerciseLocation(member) >= maxCount) {
            throw new CustomException(ErrorCode.EXERCISE_LOCATION_MAX_COUNT_VIOLATION);
        }
    }

    /**
     * 특정 회원의 삭제되지 않은 운동 위치 개수를 반환하는 메서드.
     */
    private long countMemberExerciseLocation(Member member) {
        return exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member);
    }

    /**
     * 운동 위치 이름이 중복되는지 확인하는 메서드.
     * 자신이 보유한 운동 위치 중 동일한 이름이 존재하는지 여부를 검사함.
     */
    private boolean checkDuplicateExerciseLocationName(Member member, String locationName) {
        return exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, locationName);
    }

    /**
     * 주어진 회원 ID를 사용하여 회원 정보를 조회하는 메서드.
     * 해당 회원 ID와 일치하는 회원 정보가 없을 경우 예외를 발생시킴.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
