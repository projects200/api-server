package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.GetExerciseLocationsResponse;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;

import java.util.List;

import com.project200.undabang.member.dto.record.Viewport;

public interface ExerciseLocationQueryService {
    List<GetMembersExerciseLocationsResponse> getMembersExerciseLocations(Viewport viewport);

    List<GetExerciseLocationsResponse> getExerciseLocations();
}
