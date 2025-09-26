package com.project200.undabang.member.repository;

import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;

import java.util.List;

public interface ExerciseLocationRepositoryCustom {
    List<GetMembersExerciseLocationsResponse> getMembersExerciseLocations();
}
