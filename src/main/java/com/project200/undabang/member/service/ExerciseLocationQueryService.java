package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.request.GetMembersExerciseLocationsRequest;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;

public interface ExerciseLocationQueryService {
    GetMembersExerciseLocationsResponse getMembersExerciseLocations(GetMembersExerciseLocationsRequest request);
}
