package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.exercise.repository.ExerciseTypeRepository;
import com.project200.undabang.member.dto.request.CreatePreferredExerciseRequest;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.dto.response.PreferredExerciseListResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.repository.PreferredExerciseRepository;
import com.project200.undabang.member.service.PreferredExerciseCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Override
    public PreferredExerciseListResponse createPreferredExercises(List<CreatePreferredExerciseRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (requests.size() > 5) {
            throw new CustomException(ErrorCode.PREFERRED_EXERCISE_MAX_COUNT_VIOLATION);
        }

        Member member = getMember(UserContextHolder.getUserId());
        List<PreferredExercise> existingExercises = preferredExerciseRepository
                .findAllByMemberAndPreferredExerciseDeletedAtNull(member);

        // 현재 갯수 + 요청 갯수 검증 (일단 추가하는 로직이므로 합산 검증)
        // 만약 전체 리스트 교체(Set) 로직이라면 다르게 짜야하지만, "추가(Post)" 이므로 합산 체크
        // 하지만 기획서상 목록(1~5개) 필수라고 되어있고, 보통 "내 선호 운동 설정"은 전체 덮어쓰기일 가능성도 있음.
        // 여기서는 안전하게 기존 + 신규 <= 5로 가고, 중복 체크 수행.

        if (existingExercises.size() + requests.size() > 5) {
            throw new CustomException(ErrorCode.PREFERRED_EXERCISE_MAX_COUNT_VIOLATION);
        }

        // 중복 체크를 위해 기존 ExerciseType ID 수집
        Set<Long> existingExerciseTypeIds = existingExercises.stream()
                .map(pe -> pe.getExercise().getId())
                .collect(Collectors.toSet());

        // 요청된 ExerciseTypeId 수집
        List<Long> requestExerciseIds = requests.stream()
                .map(CreatePreferredExerciseRequest::getExerciseTypeId)
                .toList();

        // 중복 요청 방지 (요청 내 중복)
        if (requestExerciseIds.stream().distinct().count() != requests.size()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE); // 요청 내 중복 존재
        }

        // 기존과 중복 방지
        for (Long id : requestExerciseIds) {
            if (existingExerciseTypeIds.contains(id)) {
                throw new CustomException(ErrorCode.PREFERRED_EXERCISE_DUPLICATED);
            }
        }

        // ExerciseType 조회
        List<ExerciseType> exerciseTypes = exerciseTypeRepository.findAllById(requestExerciseIds);
        if (exerciseTypes.size() != requestExerciseIds.size()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE); // 존재하지 않는 운동 종류 포함
        }

        Map<Long, ExerciseType> exerciseTypeMap = exerciseTypes.stream()
                .collect(Collectors.toMap(ExerciseType::getId, Function.identity()));

        List<PreferredExercise> newExercises = new ArrayList<>();

        for (CreatePreferredExerciseRequest request : requests) {
            ExerciseType exerciseType = exerciseTypeMap.get(request.getExerciseTypeId());

            PreferredExercise preferredExercise = PreferredExercise.builder()
                    .member(member)
                    .exercise(exerciseType)
                    .preferredExerciseSkillLevel(request.getSkillLevel())
                    .build();

            preferredExercise.setDaysOfWeek(request.getDaysOfWeek());
            newExercises.add(preferredExercise);
        }

        List<PreferredExercise> savedExercises = preferredExerciseRepository.saveAll(newExercises);

        List<MyPreferredExerciseResponse> responseList = savedExercises.stream()
                .map(MyPreferredExerciseResponse::from)
                .toList();

        return PreferredExerciseListResponse.from(responseList);
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
