package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.GetExerciseLocationsResponse;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;

import java.util.List;

public interface ExerciseLocationQueryService {
    List<GetMembersExerciseLocationsResponse> getMembersExerciseLocations(
            Double leftTopLatitude, Double leftTopLongitude,
            Double rightBottomLatitude, Double rightBottomLongitude);

    List<GetExerciseLocationsResponse> getExerciseLocations();
}
