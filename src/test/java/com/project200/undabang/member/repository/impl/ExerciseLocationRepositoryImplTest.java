package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
            String className = "com.project200.undabang.member.repository.impl.ExerciseLocationRepositoryImplTest$H2SpatialFunctions";
            stmt.execute(String.format("CREATE ALIAS IF NOT EXISTS ST_X FOR \"%s.getX\"", className));
            stmt.execute(String.format("CREATE ALIAS IF NOT EXISTS ST_Y FOR \"%s.getY\"", className));
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

    // ---- 헬퍼 메소드 및 H2 공간 함수 ----

    @Nested
    @DisplayName("getMembersExerciseLocations(UUID currentMemberId) 메소드는")
    class Describe_getMembersExerciseLocations_with_currentMemberId {

        @Test
        @DisplayName("currentMemberId에 해당하는 회원은 결과에서 제외한다")
        void it_excludes_current_member_from_results() {
            Member member1 = createAndSaveMember("user1", false);
            Member member2 = createAndSaveMember("user2", false);

            Picture picture1 = createAndSavePicture("http://example.com/profile1.jpg");
            Picture picture2 = createAndSavePicture("http://example.com/profile2.jpg");
            createAndSaveMemberPicture(member1, picture1);
            createAndSaveMemberPicture(member2, picture2);

            createAndSaveExerciseLocation(member1, "헬스장A", false);
            createAndSaveExerciseLocation(member2, "헬스장B", false);

            flushAndClear();

            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(member1.getMemberId());

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMemberId()).isEqualTo(member2.getMemberId());
        }

        @Test
        @DisplayName("탈퇴한 회원과 삭제된 운동장소를 제외한 모든 회원과 운동장소 정보를 반환한다")
        void it_returns_all_active_members_and_locations() {
            Member activeMember = createAndSaveMember("activeUser", false);
            Member deletedMember = createAndSaveMember("deletedUser", true);

            Picture picture = createAndSavePicture("http://example.com/profile.jpg");
            createAndSaveMemberPicture(activeMember, picture);

            ExerciseLocation activeLocation = createAndSaveExerciseLocation(activeMember, "Active Gym", false);
            createAndSaveExerciseLocation(activeMember, "Deleted Gym", true);
            createAndSaveExerciseLocation(deletedMember, "Another Gym", false);

            flushAndClear();

            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(activeMember.getMemberId());

            assertThat(results).isEmpty(); // 자기 자신 제외이므로 결과 없음
        }

        @Test
        @DisplayName("활성 상태의 운동 장소가 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_active_locations_exist() {
            Member activeMember = createAndSaveMember("activeUser", false);
            createAndSaveExerciseLocation(activeMember, "Deleted Gym", true);

            flushAndClear();

            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations(activeMember.getMemberId());

            assertThat(results).isNotNull().isEmpty();
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
        Point point = geometryFactory.createPoint(new Coordinate(127.0, 37.5));
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
}