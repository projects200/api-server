package com.project200.undabang.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetMembersExerciseLocationsRequest {
    private double latitude;
    private double longitude;
}
