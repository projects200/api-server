package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.exercise.repository.ExerciseTypeRepository;
import com.project200.undabang.member.dto.request.CreatePreferredExerciseRequest;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.repository.PreferredExerciseRepository;
import com.project200.undabang.member.service.PreferredExerciseCommandService;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PreferredExerciseCommandServiceImpl implements PreferredExerciseCommandService {

    private final MemberRepository memberRepository;
    private final PreferredExerciseRepository preferredExerciseRepository;
    private final ExerciseTypeRepository exerciseTypeRepository;
    private final PolicyService policyService; // 의존성 주입 추가

    @Override
    public List<MyPreferredExerciseResponse> createPreferredExercises(List<CreatePreferredExerciseRequest> requests) {
        int maxCount = policyService.getPolicyValueAsInt(PolicyKey.PREFERRED_EXERCISE_MAX_COUNT);

        if (requests.size() > maxCount) {
            throw new CustomException(ErrorCode.PREFERRED_EXERCISE_MAX_COUNT_VIOLATION);
        }

        Member member = getMember(UserContextHolder.getUserId());
        List<PreferredExercise> existingExercises = preferredExerciseRepository
                .findAllByMemberAndPreferredExerciseDeletedAtNull(member);

        if (existingExercises.size() + requests.size() > maxCount) {
            throw new CustomException(ErrorCode.PREFERRED_EXERCISE_MAX_COUNT_VIOLATION);
        }

        // 중복 체크를 위해 기존 ExerciseType ID 수집
        Set<Long> existingExerciseTypeIds = existingExercises.stream()
                .map(pe -> pe.getExercise().getId())
                .collect(Collectors.toSet());

        // 중복 검증
        validateDuplicates(requests, existingExerciseTypeIds);

        // 요청된 ExerciseTypeId 수집
        List<Long> requestExerciseIds = requests.stream()
                .map(CreatePreferredExerciseRequest::getExerciseTypeId)
                .toList();

        // ExerciseType 조회
        List<ExerciseType> exerciseTypes = exerciseTypeRepository.findAllById(requestExerciseIds);
        if (exerciseTypes.size() != requestExerciseIds.size()) {
            throw new CustomException(ErrorCode.PREFERRED_EXERCISE_NOT_FOUND); // 존재하지 않는 운동 종류 포함
        }

        Map<Long, ExerciseType> exerciseTypeMap = exerciseTypes.stream()
                .collect(Collectors.toMap(ExerciseType::getId, Function.identity()));

        List<PreferredExercise> newExercises = new ArrayList<>();

        for (CreatePreferredExerciseRequest request : requests) {
            ExerciseType exerciseType = exerciseTypeMap.get(request.getExerciseTypeId());

            PreferredExercise preferredExercise = PreferredExercise.createPreferredExercise(
                    member,
                    exerciseType,
                    request.getSkillLevel(),
                    request.getDaysOfWeek()
            );
            newExercises.add(preferredExercise);
        }

        List<PreferredExercise> savedExercises = preferredExerciseRepository.saveAll(newExercises);

        return savedExercises.stream()
                .map(MyPreferredExerciseResponse::from)
                .toList();
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 운동 중복 여부를 검증합니다.
     * 1. 요청 목록 내에서의 중복
     * 2. 이미 등록된 운동과의 중복
     */
    private void validateDuplicates(List<CreatePreferredExerciseRequest> requests, Set<Long> existingExerciseTypeIds) {
        Set<Long> requestIds = new HashSet<>();
        for (CreatePreferredExerciseRequest request : requests) {
            Long id = request.getExerciseTypeId();
            // 1. 요청 내 중복 검증
            // Set.add()는 이미 존재하면 false를 반환합니다.
            if (!requestIds.add(id)) {
                throw new CustomException(ErrorCode.PREFERRED_EXERCISE_DUPLICATED_IN_REQUEST);
            }
            // 2. 기존 운동과 중복 검증
            if (existingExerciseTypeIds.contains(id)) {
                throw new CustomException(ErrorCode.PREFERRED_EXERCISE_DUPLICATED);
            }
        }
    }
}
