package com.project200.undabang.member.dto.request;

import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateExerciseLocationRequest {
    @NotNull
    @Size(min = 1, max = 100, message = "운동 장소명은 최대 100글자 입력 가능합니다.")
    private String name;

    @NotNull
    @Size(min = 1, max = 255, message = "도로명/지번 주소는 최대 255자까지 입력 가능합니다.")
    private String address;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    /**
     * 주어진 회원 정보를 기반으로 ExerciseLocation 엔티티를 생성합니다.
     */
    public ExerciseLocation toEntity(Member member) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        Point point = geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));

        return ExerciseLocation.builder()
                .member(member)
                .exerciseLocationName(this.name)
                .exerciseLocationAddress(this.address)
                .exerciseLocationPoint(point)
                .exerciseLocationCreatedAt(LocalDateTime.now())
                .build();
    }
}
