package com.project200.undabang.member.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.dto.record.Viewport;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
public class ExerciseLocationRepositoryTest {

    @Autowired
    private ExerciseLocationRepository exerciseLocationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        em.createNativeQuery(
                "CREATE ALIAS IF NOT EXISTS ST_X FOR \"com.project200.undabang.member.repository.ExerciseLocationRepositoryTest.stX\"")
                .executeUpdate();
        em.createNativeQuery(
                "CREATE ALIAS IF NOT EXISTS ST_Y FOR \"com.project200.undabang.member.repository.ExerciseLocationRepositoryTest.stY\"")
                .executeUpdate();
    }

    public static Double stX(org.locationtech.jts.geom.Geometry geometry) {
        return geometry == null ? null : geometry.getCoordinate().getX();
    }

    public static Double stY(org.locationtech.jts.geom.Geometry geometry) {
        return geometry == null ? null : geometry.getCoordinate().getY();
    }

    @Test
    @DisplayName("Viewport 내부에 있는 운동 장소는 조회되어야 한다")
    void getMembersExerciseLocations_InsideViewport() {
        // given
        // 서울 시청 좌표 (대략): 위도 37.5665, 경도 126.9780
        double latitude = 37.5665;
        double longitude = 126.9780;

        Member member = createAndSaveMember("서울시민", "seoul@test.com");
        createAndSaveExerciseLocation(member, "시청 헬스장", latitude, longitude);

        // Viewport: 서울 전체를 대략 포함 (좌상단: 37.7, 126.8 / 우하단: 37.4, 127.2)
        double leftTopLat = 37.7;
        double leftTopLon = 126.8;
        double rightBottomLat = 37.4;
        double rightBottomLon = 127.2;

        // when
        List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                Collections.emptySet(),
                new Viewport(leftTopLat, leftTopLon, rightBottomLat, rightBottomLon));

        // then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getLocations()).hasSize(1);
        assertThat(results.get(0).getNickname()).isEqualTo("서울시민");
    }

    @Test
    @DisplayName("Viewport 외부에 있는 운동 장소는 조회되지 않아야 한다")
    void getMembersExerciseLocations_OutsideViewport() {
        // given
        // 부산 좌표 (대략): 위도 35.1796, 경도 129.0756
        double latitude = 35.1796;
        double longitude = 129.0756;

        Member member = createAndSaveMember("부산시민", "busan@test.com");
        createAndSaveExerciseLocation(member, "해운대 헬스장", latitude, longitude);

        // Viewport: 서울 지역 (좌상단: 37.7, 126.8 / 우하단: 37.4, 127.2)
        double leftTopLat = 37.7;
        double leftTopLon = 126.8;
        double rightBottomLat = 37.4;
        double rightBottomLon = 127.2;

        // when
        List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                Collections.emptySet(),
                new Viewport(leftTopLat, leftTopLon, rightBottomLat, rightBottomLon));

        // then
        assertThat(results).isEmpty();
    }

    /**
     * 좌표 매핑 검증 (X=경도, Y=위도)
     * 만약 RepositoryImpl에서 ST_X(경도)를 위도 범위와 비교하고 있다면 이 테스트는 실패할 것입니다.
     */
    @Test
    @DisplayName("경도(X)와 위도(Y)가 올바르게 매핑되어 조회되어야 한다")
    void getMembersExerciseLocations_CoordinateMapping() {
        // given
        // 특정 위치: 위도 10.0, 경도 50.0
        // (일반적인 한국 좌표가 아닌, 위도/경도가 확실히 구분되는 값 사용)
        double latitude = 10.0;
        double longitude = 50.0;

        Member member = createAndSaveMember("테스트유저", "test@test.com");
        createAndSaveExerciseLocation(member, "테스트 헬스장", latitude, longitude);

        // Viewport를 해당 위치가 포함되도록 설정
        // 위도 범위: 9.0 ~ 11.0
        // 경도 범위: 49.0 ~ 51.0
        double leftTopLat = 11.0;
        double leftTopLon = 49.0;
        double rightBottomLat = 9.0;
        double rightBottomLon = 51.0;

        // when
        List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                Collections.emptySet(),
                new Viewport(leftTopLat, leftTopLon, rightBottomLat, rightBottomLon));

        // then
        // 만약 버그가 있다면 (X=50.0 을 위도 범위 9.0~11.0 와 비교) -> 실패(조회 안됨) 할 것임.
        assertThat(results).isNotEmpty();
    }

    private Member createAndSaveMember(String nickname, String email) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(email)
                .memberNickname(nickname)
                .memberGender(MemberGender.MALE)
                .memberBday(LocalDate.of(1990, 1, 1))
                .memberScore((byte) 50)
                .build();
        memberRepository.save(member);
        return member;
    }

    private ExerciseLocation createAndSaveExerciseLocation(Member member, String name, double latitude,
            double longitude) {
        // Point(x, y) = Point(longitude, latitude)
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326); // WGS84

        ExerciseLocation location = ExerciseLocation.builder()
                .member(member)
                .exerciseLocationName(name)
                .exerciseLocationAddress("주소")
                .exerciseLocationPoint(point)
                .build();
        exerciseLocationRepository.save(location);
        return location;
    }
}
