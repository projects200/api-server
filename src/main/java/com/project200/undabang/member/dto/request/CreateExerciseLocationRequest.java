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
    /**
     * SRID 4326 (WGS84) 전용 GeometryFactory. 매 호출마다 생성하면 Hibernate Spatial 이
     * 좌표계를 반복 파싱하면서 geolatte CRS 객체 + ANTLR 파서 state 가 Metaspace 에 누적됨.
     * JTS GeometryFactory 는 thread-safe 이므로 정적 공유 안전.
     */
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

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
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(this.longitude, this.latitude));

        return ExerciseLocation.builder()
                .member(member)
                .exerciseLocationName(this.name)
                .exerciseLocationAddress(this.address)
                .exerciseLocationPoint(point)
                .exerciseLocationCreatedAt(LocalDateTime.now())
                .build();
    }
}
