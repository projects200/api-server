package com.project200.undabang.member.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.dto.record.Viewport;
import com.project200.undabang.member.dto.response.GetOtherMemberExerciseLocationsResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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

    @BeforeAll
    static void setupH2Geometry(@Autowired DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String className = H2SpatialFunctions.class.getName();

            // 기본 ST_X, ST_Y 등록
            stmt.execute("CREATE ALIAS IF NOT EXISTS ST_X FOR \"" + className + ".getX\"");
            stmt.execute("CREATE ALIAS IF NOT EXISTS ST_Y FOR \"" + className + ".getY\"");

            // MySQL 8.0 전용 함수인 ST_Latitude, ST_Longitude를 H2용 함수로 연결
            // H2는 함수명을 대문자로 찾는 경우가 많으므로 대문자로 등록합니다.
            stmt.execute("CREATE ALIAS IF NOT EXISTS ST_LATITUDE FOR \"" + className + ".getY\"");   // 위도 = Y
            stmt.execute("CREATE ALIAS IF NOT EXISTS ST_LONGITUDE FOR \"" + className + ".getX\"");  // 경도 = X

            // 혹시 모를 소문자 호출 대비
            stmt.execute("CREATE ALIAS IF NOT EXISTS ST_latitude FOR \"" + className + ".getY\"");
            stmt.execute("CREATE ALIAS IF NOT EXISTS ST_longitude FOR \"" + className + ".getX\"");
        }
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
        List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
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
        List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
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
        List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
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

    private ExerciseLocation createAndSaveExerciseLocation(Member member, String name, double latitude, double longitude) {
        double coordinateX = longitude;
        double coordinateY = latitude;

        Point point = geometryFactory.createPoint(new Coordinate(coordinateX, coordinateY));
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

    public static class H2SpatialFunctions {
        private static final WKBReader wkbReader = new WKBReader();

        // 경도 (Longitude / X)
        public static double getX(byte[] wkb) {
            if (wkb == null) return 0.0;
            try {
                Geometry geom = wkbReader.read(wkb);
                return (geom instanceof Point) ? ((Point) geom).getX() : 0.0;
            } catch (ParseException e) {
                throw new RuntimeException("Failed to parse WKB for ST_X/ST_Longitude", e);
            }
        }

        // 위도 (Latitude / Y)
        public static double getY(byte[] wkb) {
            if (wkb == null) return 0.0;
            try {
                Geometry geom = wkbReader.read(wkb);
                return (geom instanceof Point) ? ((Point) geom).getY() : 0.0;
            } catch (ParseException e) {
                throw new RuntimeException("Failed to parse WKB for ST_Y/ST_Latitude", e);
            }
        }
    }
}
