package com.project200.undabang.member.service.impl;

import com.project200.undabang.member.dto.request.GetMembersExerciseLocationsRequest;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.service.ExerciseLocationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseLocationQueryServiceImpl implements ExerciseLocationQueryService {
    private final ExerciseLocationRepository exerciseLocationRepository;

    @Override
    public GetMembersExerciseLocationsResponse getMembersExerciseLocations(GetMembersExerciseLocationsRequest request) {


        return null;
    }

}
