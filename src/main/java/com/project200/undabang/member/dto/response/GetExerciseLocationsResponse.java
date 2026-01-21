package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.entity.ExerciseLocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 운동 장소 정보를 반환하기 위한 응답 DTO 클래스.
 * <p>
 * 이 클래스는 특정 운동 장소의 ID, 이름, 주소, 위도 및 경도 정보를 포함합니다.
 * 운동 장소 객체를 응답 객체로 변환하여 클라이언트에 전달할 때 사용됩니다.
 * <p>
 * 주요 기능:
 * - 운동 장소 ID, 이름, 주소, 위도, 경도를 저장합니다.
 * - {@link ExerciseLocation} 엔티티를 {@code GetExerciseLocationsResponse} 객체로 변환하는 정적 메서드를 제공합니다.
 * <p>
 * 필드:
 * - id: 운동 장소의 고유 ID를 나타냅니다.
 * - name: 운동 장소의 이름 또는 상호명을 나타냅니다.
 * - address: 운동 장소의 도로명 주소를 나타냅니다.
 * - latitude: 운동 장소의 위도를 나타냅니다 (GPS 좌표).
 * - longitude: 운동 장소의 경도를 나타냅니다 (GPS 좌표).
 * <p>
 * 메서드:
 * - {@code from(ExerciseLocation exerciseLocation)}:
 * 주어진 {@link ExerciseLocation} 엔티티를 기반으로 응답 객체를 생성합니다.
 * - Point 객체의 X 값은 경도(longitude)와 매핑됩니다.
 * - Point 객체의 Y 값은 위도(latitude)와 매핑됩니다.
 */
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
                // Point 의 X 좌표는 경도와 매핑됨
                .latitude(exerciseLocation.getExerciseLocationPoint().getX())
                // Point 의 Y 좌표는 위도와 매핑됨
                .longitude(exerciseLocation.getExerciseLocationPoint().getY())
                .build();
    }
}
