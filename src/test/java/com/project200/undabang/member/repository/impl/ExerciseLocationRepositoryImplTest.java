package com.project200.undabang.member.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.record.Viewport;
import com.project200.undabang.member.dto.response.GetOtherMemberExerciseLocationsResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ExerciseLocationRepositoryImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private ExerciseLocationRepository exerciseLocationRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeAll
    static void setupH2Geometry(@Autowired DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String className = H2SpatialFunctions.class.getName();

            // QueryDSL용 ST_Latitude(Y), ST_Longitude(X)
            stmt.execute(String.format("CREATE ALIAS IF NOT EXISTS ST_Latitude FOR \"%s.getY\"", className));
            stmt.execute(String.format("CREATE ALIAS IF NOT EXISTS ST_Longitude FOR \"%s.getX\"", className));
        }
    }

    // =========================================================================
    // 테스트 케이스 영역 (비즈니스 로직 검증)
    // =========================================================================

    @Nested
    @DisplayName("countByMemberAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_countByMemberAndExerciseLocationDeletedAtNull {

        @Test
        @DisplayName("해당 회원의 삭제되지 않은 운동 장소의 개수만 정확히 반환한다")
        void it_returns_the_count_of_active_locations_only() {
            Member member1 = createAndSaveMember("user1", false);
            Member member2 = createAndSaveMember("user2", false);

            createAndSaveExerciseLocation(member1, "Active Gym 1", false);
            createAndSaveExerciseLocation(member1, "Active Gym 2", false);
            createAndSaveExerciseLocation(member1, "Deleted Gym", true);
            createAndSaveExerciseLocation(member2, "Another Gym", false);

            flushAndClear();

            long count = exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member1);

            assertThat(count).isEqualTo(2L);
        }

        @Test
        @DisplayName("해당 회원의 모든 운동 장소가 삭제되었다면 0을 반환한다")
        void it_returns_zero_if_all_locations_are_deleted() {
            Member member = createAndSaveMember("user", false);
            createAndSaveExerciseLocation(member, "Deleted Gym A", true);
            createAndSaveExerciseLocation(member, "Deleted Gym B", true);
            flushAndClear();

            long count = exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member);

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("해당 회원이 운동 장소를 가지고 있지 않다면 0을 반환한다")
        void it_returns_zero_if_member_has_no_locations() {
            Member member = createAndSaveMember("userWithNoLocations", false);
            flushAndClear();

            long count = exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member);

            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("findAllByMemberAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_findAllByMemberAndExerciseLocationDeletedAtNull {

        @Test
        @DisplayName("해당 회원의 삭제되지 않은 운동 장소 목록만 반환한다")
        void it_returns_only_active_locations_for_that_member() {
            Member member1 = createAndSaveMember("user1", false);
            Member member2 = createAndSaveMember("user2", false);

            createAndSaveExerciseLocation(member1, "Active Gym 1", false);
            createAndSaveExerciseLocation(member1, "Active Gym 2", false);
            createAndSaveExerciseLocation(member1, "Deleted Gym", true);
            createAndSaveExerciseLocation(member2, "Another Active Gym", false);

            flushAndClear();

            List<ExerciseLocation> results = exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member1);

            assertThat(results).hasSize(2).extracting(ExerciseLocation::getExerciseLocationName).containsExactlyInAnyOrder("Active Gym 1", "Active Gym 2");
        }

        @Test
        @DisplayName("해당 회원의 모든 운동 장소가 삭제된 상태라면 빈 리스트를 반환한다")
        void it_returns_empty_list_if_all_locations_are_deleted() {
            Member member = createAndSaveMember("userWithDeletedLocations", false);
            createAndSaveExerciseLocation(member, "Deleted Gym A", true);
            createAndSaveExerciseLocation(member, "Deleted Gym B", true);

            flushAndClear();

            List<ExerciseLocation> results = exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member);

            assertThat(results).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("해당 회원이 운동 장소를 전혀 가지고 있지 않다면 빈 리스트를 반환한다")
        void it_returns_empty_list_if_member_has_no_locations() {
            Member member = createAndSaveMember("userWithNoLocations", false);
            flushAndClear();

            List<ExerciseLocation> results = exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member);

            assertThat(results).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("findByExerciseLocationIdAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_findByExerciseLocationIdAndExerciseLocationDeletedAtNull {

        @Test
        @DisplayName("삭제되지 않은 운동 장소를 ID로 조회하면 Optional에 담아 반환한다")
        void it_returns_location_when_exists_and_not_deleted() {
            Member member = createAndSaveMember("user", false);
            ExerciseLocation location = createAndSaveExerciseLocation(member, "활성 헬스장", false);
            flushAndClear();

            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(location.getExerciseLocationId());

            assertThat(result).isPresent();
            assertThat(result.get().getExerciseLocationId()).isEqualTo(location.getExerciseLocationId());
            assertThat(result.get().getExerciseLocationDeletedAt()).isNull();
        }

        @Test
        @DisplayName("ID에 해당하는 운동 장소가 삭제된 경우 Optional.empty를 반환한다")
        void it_returns_empty_when_location_is_deleted() {
            Member member = createAndSaveMember("user", false);
            ExerciseLocation location = createAndSaveExerciseLocation(member, "삭제된 헬스장", true);
            flushAndClear();

            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(location.getExerciseLocationId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ID에 해당하는 운동 장소가 존재하지 않는 경우 Optional.empty를 반환한다")
        void it_returns_empty_when_location_does_not_exist() {
            Long nonExistentId = 999L;

            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(nonExistentId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull {

        @Test
        @DisplayName("삭제되지 않은 운동 장소 중 동일한 이름이 존재하면 true를 반환한다")
        void it_returns_true_when_active_location_with_same_name_exists() {
            Member member = createAndSaveMember("user", false);
            String existingName = "Active Gym";
            createAndSaveExerciseLocation(member, existingName, false);
            flushAndClear();

            boolean result = exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, existingName);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("동일한 이름의 운동 장소가 존재하지만 삭제된 상태이면 false를 반환한다")
        void it_returns_false_when_location_with_same_name_is_deleted() {
            Member member = createAndSaveMember("user", false);
            String deletedName = "Deleted Gym";
            createAndSaveExerciseLocation(member, deletedName, true);
            flushAndClear();

            boolean result = exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, deletedName);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("동일한 이름의 운동 장소가 존재하지 않으면 false를 반환한다")
        void it_returns_false_when_no_location_with_same_name_exists() {
            Member member = createAndSaveMember("user", false);
            createAndSaveExerciseLocation(member, "Some Other Gym", false);
            flushAndClear();

            boolean result = exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, "NonExistent Gym");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_findByExerciseLocationIdAndMemberIdAndDeletedAtNull {

        @Test
        @DisplayName("삭제되지 않은 운동 장소를 ID와 회원 ID로 조회하면 Optional에 담아 반환한다")
        void it_returns_location_when_id_and_member_match_and_not_deleted() {
            Member member = createAndSaveMember("user", false);
            ExerciseLocation location = createAndSaveExerciseLocation(member, "활성 헬스장", false);
            flushAndClear();

            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(location.getExerciseLocationId(), member.getMemberId());

            assertThat(result).isPresent();
            assertThat(result.get().getExerciseLocationId()).isEqualTo(location.getExerciseLocationId());
            assertThat(result.get().getMember().getMemberId()).isEqualTo(member.getMemberId());
        }

        @Test
        @DisplayName("운동 장소 ID는 같지만 회원 ID가 다르면 Optional.empty를 반환한다")
        void it_returns_empty_when_member_id_does_not_match() {
            Member owner = createAndSaveMember("owner", false);
            Member other = createAndSaveMember("other", false);
            ExerciseLocation location = createAndSaveExerciseLocation(owner, "헬스장", false);
            flushAndClear();

            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(location.getExerciseLocationId(), other.getMemberId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("운동 장소가 삭제된 상태라면 Optional.empty를 반환한다")
        void it_returns_empty_when_location_is_deleted() {
            Member member = createAndSaveMember("user", false);
            ExerciseLocation location = createAndSaveExerciseLocation(member, "삭제된 헬스장", true);
            flushAndClear();

            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(location.getExerciseLocationId(), member.getMemberId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("운동 장소 ID가 존재하지 않으면 Optional.empty를 반환한다")
        void it_returns_empty_when_location_id_does_not_exist() {
            Member member = createAndSaveMember("user", false);
            Long nonExistentLocationId = 9999L;
            flushAndClear();

            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(nonExistentLocationId, member.getMemberId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMembersExerciseLocations(Set<UUID> excludeMemberIdSet) 메소드는")
    class Describe_getMembersExerciseLocations_with_exclusionSet {

        private Member currentUser, otherUser1, otherUser2, otherUser3;
        private ExerciseLocation otherUser1Loc;

        @BeforeEach
        void setup() {
            currentUser = createAndSaveMember("currentUser", false);
            otherUser1 = createAndSaveMember("otherUser1", false);
            otherUser2 = createAndSaveMember("otherUser2", false);
            otherUser3 = createAndSaveMember("otherUser3", false);

            otherUser1Loc = createAndSaveExerciseLocation(otherUser1, "User1 Gym", false);
            createAndSaveExerciseLocation(otherUser2, "User2 Gym", false);
            createAndSaveExerciseLocation(otherUser3, "User3 Gym", false);
        }

        @Test
        @DisplayName("주어진 제외 목록(Set)에 포함된 회원들을 결과에서 모두 제외한다")
        void it_excludes_all_members_in_the_given_exclusion_set() {
            Set<UUID> exclusionIds = Set.of(currentUser.getMemberId(), otherUser2.getMemberId());
            flushAndClear();

            List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(exclusionIds, new Viewport(38.0, 126.0, 37.0, 128.0));

            assertThat(results).hasSize(2).extracting(GetOtherMemberExerciseLocationsResponse::getMemberId).containsExactlyInAnyOrder(otherUser1.getMemberId(), otherUser3.getMemberId());
        }

        @Test
        @DisplayName("나, 내가 차단한 회원, 나를 차단한 회원을 모두 제외하고 결과를 반환한다")
        void it_excludes_self_i_blocked_and_blocked_by_members() {
            createAndSaveMemberBlock(currentUser, otherUser2, false);
            createAndSaveMemberBlock(otherUser3, currentUser, false);

            Set<UUID> exclusionIds = new HashSet<>();
            exclusionIds.add(currentUser.getMemberId());
            exclusionIds.add(otherUser2.getMemberId());
            exclusionIds.add(otherUser3.getMemberId());

            flushAndClear();

            List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(exclusionIds, new Viewport(38.0, 126.0, 37.0, 128.0));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMemberId()).isEqualTo(otherUser1.getMemberId());
        }

        @Test
        @DisplayName("차단했다가 해제한 회원은 결과에 포함시킨다")
        void it_includes_unblocked_members_in_results() {
            createAndSaveMemberBlock(currentUser, otherUser2, true);
            Set<UUID> exclusionIds = Set.of(currentUser.getMemberId());
            flushAndClear();

            List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(exclusionIds, new Viewport(38.0, 126.0, 37.0, 128.0));

            assertThat(results).hasSize(3).extracting(GetOtherMemberExerciseLocationsResponse::getMemberId).containsExactlyInAnyOrder(otherUser1.getMemberId(), otherUser2.getMemberId(), otherUser3.getMemberId());
        }

        @Test
        @DisplayName("제외 목록이 비어있으면 모든 회원의 위치를 반환하며, Location ID가 올바르게 매핑되었는지 확인한다")
        void it_returns_all_locations_if_exclusion_set_is_empty_and_checks_mapping() {
            Set<UUID> exclusionIds = Collections.emptySet();
            flushAndClear();

            List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(exclusionIds, new Viewport(38.0, 126.0, 37.0, 128.0));

            assertThat(results).hasSize(3);

            GetOtherMemberExerciseLocationsResponse targetResponse = results.stream().filter(r -> r.getMemberId().equals(otherUser1.getMemberId())).findFirst().orElseThrow();

            assertThat(targetResponse.getLocations()).hasSize(1);
            assertThat(targetResponse.getLocations().iterator().next().exerciseLocationId()).isEqualTo(otherUser1Loc.getExerciseLocationId());
        }
    }

    @Nested
    @DisplayName("getMembersExerciseLocations(..., bounds) 데이터 필터링은")
    class Describe_getMembersExerciseLocations_filtering {

        @Test
        @DisplayName("주어진 경계 내에 있는 운동 장소만 반환한다")
        void it_returns_locations_within_bounds_only() {
            Member insideMember = createAndSaveMember("insideUser", false);
            createAndSaveExerciseLocation(insideMember, "Inside Gym", 127.0, 37.5, false);

            Member outsideMember = createAndSaveMember("outsideUser", false);
            createAndSaveExerciseLocation(outsideMember, "Outside Gym", 129.0, 39.0, false);

            flushAndClear();

            // Box: LeftTop(38.0, 126.0) ~ RightBottom(37.0, 128.0)
            List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                    Collections.emptySet(),
                    new Viewport(38.0, 126.0, 37.0, 128.0));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getLocations())
                    .extracting(ExerciseLocationRecord::exerciseLocationName)
                    .containsExactly("Inside Gym");
        }
    }

    // =========================================================================
    // 헬퍼 메소드 및 유틸리티 클래스 영역
    // =========================================================================

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private ExerciseLocation createAndSaveExerciseLocation(Member member, String name, boolean deleted) {
        // 기본 좌표값(서울 어딘가)
        return createAndSaveExerciseLocation(member, name, 127.0, 37.5, deleted);
    }

    private ExerciseLocation createAndSaveExerciseLocation(Member member, String name, double lon, double lat, boolean deleted) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        point.setSRID(4326);

        ExerciseLocation location = ExerciseLocation.builder()
                .member(member)
                .exerciseLocationName(name)
                .exerciseLocationAddress("Some Address")
                .exerciseLocationPoint(point)
                .exerciseLocationDeletedAt(deleted ? LocalDateTime.now() : null)
                .build();
        em.persist(location);
        return location;
    }

    private Member createAndSaveMember(String nickname, boolean deleted) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(2000, 1, 1))
                .memberDeletedAt(deleted ? LocalDateTime.now() : null)
                .build();
        em.persist(member);
        return member;
    }

    private void createAndSaveMemberBlock(Member blocker, Member blocked, boolean unblocked) {
        MemberBlock memberBlock = MemberBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .memberBlockDeletedAt(unblocked ? LocalDateTime.now() : null)
                .build();
        em.persist(memberBlock);
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