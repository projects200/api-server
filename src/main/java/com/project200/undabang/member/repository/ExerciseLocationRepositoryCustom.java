package com.project200.undabang.member.repository;

import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;

import java.util.List;
import java.util.UUID;

public interface ExerciseLocationRepositoryCustom {
    List<GetMembersExerciseLocationsResponse> getMembersExerciseLocations(UUID currentMemberId);
}
