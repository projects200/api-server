package com.project200.undabang.member.service.impl;

import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.service.ExerciseLocationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseLocationQueryServiceImpl implements ExerciseLocationQueryService {
    private final ExerciseLocationRepository exerciseLocationRepository;

    /**
     * 회원들의 운동 위치 정보를 조회하여 반환합니다.
     *
     * @return 회원들의 운동 위치 정보를 담고 있는 GetMembersExerciseLocationsResponse 객체 리스트
     */
    @Override
    public List<GetMembersExerciseLocationsResponse> getMembersExerciseLocations() {

        return exerciseLocationRepository.getMembersExerciseLocations();
    }
}
