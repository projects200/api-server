package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.record.Viewport;
import com.project200.undabang.member.dto.response.GetExerciseLocationsResponse;
import com.project200.undabang.member.dto.response.GetOtherMemberExerciseLocationsResponse;

import java.util.List;

public interface ExerciseLocationQueryService {
    List<GetOtherMemberExerciseLocationsResponse> getMembersExerciseLocations(Viewport viewport);
    List<GetExerciseLocationsResponse> getExerciseLocations();
}
