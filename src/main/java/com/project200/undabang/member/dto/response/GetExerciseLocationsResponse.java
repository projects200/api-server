package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.entity.ExerciseLocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetExerciseLocationsResponse {
    private Long id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;

    public static GetExerciseLocationsResponse from(ExerciseLocation exerciseLocation) {
        return GetExerciseLocationsResponse.builder()
                .id(exerciseLocation.getExerciseLocationId())
                .name(exerciseLocation.getExerciseLocationName())
                .address(exerciseLocation.getExerciseLocationAddress())
                // Point 의 X 좌표는 위도와 매핑됨
                .latitude(exerciseLocation.getExerciseLocationPoint().getX())
                // Point 의 Y 좌표는 경도와 매핑됨
                .longitude(exerciseLocation.getExerciseLocationPoint().getY())
                .build();
    }
}
