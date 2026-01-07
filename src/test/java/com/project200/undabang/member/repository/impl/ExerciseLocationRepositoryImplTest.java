package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
import com.project200.undabang.member.entity.MemberPicture;
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


    // ... 기존 @Nested 클래스들 아래에 추가 ...

    @BeforeAll
    static void setupH2Geometry(@Autowired DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String className = "com.project200.undabang.member.repository.impl.ExerciseLocationRepositoryImplTest$H2SpatialFunctions";
            stmt.execute(String.format("CREATE ALIAS IF NOT EXISTS ST_X FOR \"%s.getX\"", className));
            stmt.execute(String.format("CREATE ALIAS IF NOT EXISTS ST_Y FOR \"%s.getY\"", className));
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

            List<ExerciseLocation> results = exerciseLocationRepository
                    .findAllByMemberAndExerciseLocationDeletedAtNull(member1);

            assertThat(results).hasSize(2)
                    .extracting(ExerciseLocation::getExerciseLocationName)
                    .containsExactlyInAnyOrder("Active Gym 1", "Active Gym 2");
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

    private ExerciseLocation createAndSaveExerciseLocation(Member member, String name, boolean deleted) {
        return createAndSaveExerciseLocation(member, name, 127.0, 37.5, deleted);
    }

    private ExerciseLocation createAndSaveExerciseLocation(Member member, String name, double lon, double lat,
            boolean deleted) {
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

    private void flushAndClear() {
        em.flush();
        em.clear();
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

    private Picture createAndSavePicture(String url) {
        Picture picture = Picture.builder()
                .pictureUrl(url)
                .build();
        em.persist(picture);
        em.flush();
        return picture;
    }

    private void createAndSaveMemberPicture(Member member, Picture picture) {
        MemberPicture memberPicture = MemberPicture.builder()
                .member(member)
                .picture(picture)
                .memberPicturesUrl(picture.getPictureUrl())
                .build();
        em.persist(memberPicture);

        member.updateProfilePicture(memberPicture);
        em.persist(member);
    }

    private void createAndSaveMemberBlock(Member blocker, Member blocked, boolean unblocked) {
        MemberBlock memberBlock = MemberBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .memberBlockDeletedAt(unblocked ? LocalDateTime.now() : null)
                .build();
        em.persist(memberBlock);
    }

    // H2용 공간 함수
    public static class H2SpatialFunctions {

        private static final WKBReader wkbReader = new WKBReader();

        public static double getX(byte[] wkb) {
            if (wkb == null) {
                return 0.0;
            }
            try {
                Geometry geom = wkbReader.read(wkb);
                if (geom instanceof Point) {
                    return ((Point) geom).getX();
                }
            } catch (ParseException e) {
                throw new RuntimeException("Failed to parse WKB for ST_X", e);
            }
            return 0.0;
        }

        public static double getY(byte[] wkb) {
            if (wkb == null) {
                return 0.0;
            }
            try {
                Geometry geom = wkbReader.read(wkb);
                if (geom instanceof Point) {
                    return ((Point) geom).getY();
                }
            } catch (ParseException e) {
                throw new RuntimeException("Failed to parse WKB for ST_Y", e);
            }
            return 0.0;
        }
    }

    @Nested
    @DisplayName("getMembersExerciseLocations(Set<UUID> excludeMemberIdSet) 메소드는")
    class Describe_getMembersExerciseLocations_with_exclusionSet {

        private Member currentUser, otherUser1, otherUser2, otherUser3;

        @BeforeEach
        void setup() {
            // 테스트에 사용할 공통 회원 생성
            currentUser = createAndSaveMember("currentUser", false);
            otherUser1 = createAndSaveMember("otherUser1", false); // 정상 조회 대상
            otherUser2 = createAndSaveMember("otherUser2", false); // 내가 차단할 대상
            otherUser3 = createAndSaveMember("otherUser3", false); // 나를 차단할 대상

            // 모든 회원이 운동 장소를 가지도록 설정
            createAndSaveExerciseLocation(currentUser, "My Gym", false);
            createAndSaveExerciseLocation(otherUser1, "User1 Gym", false);
            createAndSaveExerciseLocation(otherUser2, "User2 Gym", false);
            createAndSaveExerciseLocation(otherUser3, "User3 Gym", false);
        }

        @Test
        @DisplayName("주어진 제외 목록(Set)에 포함된 회원들을 결과에서 모두 제외한다")
        void it_excludes_all_members_in_the_given_exclusion_set() {
            // given
            Set<UUID> exclusionIds = Set.of(
                    currentUser.getMemberId(), // 1. 나 자신
                    otherUser2.getMemberId()   // 2. 내가 차단한 회원
            );

            flushAndClear();

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                    exclusionIds,
                    38.0, 126.0,
                    37.0, 128.0);

            // then
            assertThat(results).hasSize(2)
                    .extracting(GetMembersExerciseLocationsResponse::getMemberId)
                    .containsExactlyInAnyOrder(otherUser1.getMemberId(), otherUser3.getMemberId());
        }

        @Test
        @DisplayName("나, 내가 차단한 회원, 나를 차단한 회원을 모두 제외하고 결과를 반환한다")
        void it_excludes_self_i_blocked_and_blocked_by_members() {
            // given: 차단 관계 설정
            // currentUser가 otherUser2를 차단
            createAndSaveMemberBlock(currentUser, otherUser2, false);
            // otherUser3이 currentUser를 차단
            createAndSaveMemberBlock(otherUser3, currentUser, false);

            // 서비스 레이어에서 수행할 로직을 테스트에서 직접 구성
            Set<UUID> exclusionIds = new HashSet<>();
            exclusionIds.add(currentUser.getMemberId());    // 나
            exclusionIds.add(otherUser2.getMemberId()); // 내가 차단한 사람
            exclusionIds.add(otherUser3.getMemberId()); // 나를 차단한 사람

            flushAndClear();

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                    exclusionIds,
                    38.0, 126.0,
                    37.0, 128.0);

            // then
            // otherUser1만 조회되어야 함
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMemberId()).isEqualTo(otherUser1.getMemberId());
        }

        @Test
        @DisplayName("차단했다가 해제한 회원은 결과에 포함시킨다")
        void it_includes_unblocked_members_in_results() {
            // given: otherUser2를 차단했다가 해제한 상황
            createAndSaveMemberBlock(currentUser, otherUser2, true); // unblocked = true

            // 서비스 레이어에서는 차단 해제된 otherUser2를 제외 목록에 포함시키지 않을 것임.
            Set<UUID> exclusionIds = Set.of(currentUser.getMemberId()); // 나 자신만 제외

            flushAndClear();

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                    exclusionIds,
                    38.0, 126.0,
                    37.0, 128.0);

            // then
            // otherUser1, otherUser2, otherUser3 모두 조회되어야 함
            assertThat(results).hasSize(3)
                    .extracting(GetMembersExerciseLocationsResponse::getMemberId)
                    .containsExactlyInAnyOrder(
                            otherUser1.getMemberId(),
                            otherUser2.getMemberId(),
                            otherUser3.getMemberId()
                    );
        }

        @Test
        @DisplayName("제외 목록이 비어있으면 모든 회원(자기 자신 포함)의 위치를 반환한다")
        void it_returns_all_locations_if_exclusion_set_is_empty() {
            // given
            Set<UUID> exclusionIds = Collections.emptySet();
            flushAndClear();

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                    exclusionIds,
                    38.0, 126.0,
                    37.0, 128.0);

            // then
            assertThat(results).hasSize(4)
                    .extracting(GetMembersExerciseLocationsResponse::getMemberId)
                    .containsExactlyInAnyOrder(
                            currentUser.getMemberId(),
                            otherUser1.getMemberId(),
                            otherUser2.getMemberId(),
                            otherUser3.getMemberId());
        }
    }

    @Nested
    @DisplayName("getMembersExerciseLocations(Set<UUID> excludeMemberIdSet, bounds...) 데이터 필터링은")
    class Describe_getMembersExerciseLocations_filtering {

        @Test
        @DisplayName("주어진 경계 내에 있는 운동 장소만 반환한다")
        void it_returns_locations_within_bounds_only() {
            // given
            Member insideMember = createAndSaveMember("insideUser", false);
            createAndSaveExerciseLocation(insideMember, "Inside Gym", 127.0, 37.5, false);

            Member outsideMember = createAndSaveMember("outsideUser", false);
            createAndSaveExerciseLocation(outsideMember, "Outside Gym", 129.0, 39.0, false);

            flushAndClear();

            // when
            // Box: LeftTop(38.0, 126.0) ~ RightBottom(37.0, 128.0)
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(
                    Collections.emptySet(),
                    38.0, 126.0, // LeftTop
                    37.0, 128.0 // RightBottom
            );

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getLocations())
                    .extracting(ExerciseLocationRecord::exerciseLocationName)
                    .containsExactly("Inside Gym");
        }
    }
}