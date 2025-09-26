package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
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
            // H2에 ST_X, ST_Y 함수를 생성합니다.
            // 중첩 클래스를 참조하기 위해 '$'를 사용합니다.
            String className = "com.project200.undabang.member.repository.impl.ExerciseLocationRepositoryImplTest$H2SpatialFunctions";
            stmt.execute(String.format("CREATE ALIAS IF NOT EXISTS ST_X FOR \"%s.getX\"", className));
            stmt.execute(String.format("CREATE ALIAS IF NOT EXISTS ST_Y FOR \"%s.getY\"", className));
        }
    }

    private Picture createAndSavePicture(String url) {
        Picture picture = Picture.builder()
                .pictureUrl(url)
                .build();
        em.persist(picture);
        em.flush(); // Picture의 ID 생성을 위해 flush
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

    private Member createAndSaveMember(String nickname) {
        return createAndSaveMember(nickname, false);
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
                // 테스트 실패를 유도하거나 기본값을 반환
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
                // 테스트 실패를 유도하거나 기본값을 반환
                throw new RuntimeException("Failed to parse WKB for ST_Y", e);
            }
            return 0.0;
        }
    }

    @Nested
    @DisplayName("getMembersExerciseLocations 메소드는")
    class Describe_getMembersExerciseLocations {

        @Test
        @DisplayName("탈퇴한 회원과 삭제된 운동장소를 제외한 모든 회원과 운동장소 정보를 반환한다")
        void it_returns_all_active_members_and_locations() {
            // given
            Member activeMember = createAndSaveMember("activeUser", false);
            Member deletedMember = createAndSaveMember("deletedUser", true);

            Picture picture = createAndSavePicture("http://example.com/profile.jpg");
            createAndSaveMemberPicture(activeMember, picture);

            ExerciseLocation activeLocation = createAndSaveExerciseLocation(activeMember, "Active Gym", false);
            createAndSaveExerciseLocation(activeMember, "Deleted Gym", true);
            createAndSaveExerciseLocation(deletedMember, "Another Gym", false);

            flushAndClear();

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations();

            // then
            assertThat(results).hasSize(1);

            GetMembersExerciseLocationsResponse result = results.get(0);
            assertThat(result.getMemberId()).isEqualTo(activeMember.getMemberId());
            assertThat(result.getNickname()).isEqualTo(activeMember.getMemberNickname());
            assertThat(result.getProfileThumbnailUrl()).isEqualTo(activeMember.getMemberPicture().getMemberPicturesUrl());
            assertThat(result.getProfileImageUrl()).isEqualTo(picture.getPictureUrl());
            assertThat(result.getLocations()).hasSize(1);

            ExerciseLocationRecord locationRecord = result.getLocations().get(0);
            assertThat(locationRecord.exerciseLocationName()).isEqualTo(activeLocation.getExerciseLocationName());
            assertThat(locationRecord.latitude()).isEqualTo(37.5);
            assertThat(locationRecord.longitude()).isEqualTo(127.0);
        }

        @Test
        @DisplayName("활성 상태의 운동 장소가 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_active_locations_exist() {
            // given
            Member activeMember = createAndSaveMember("activeUser", false);
            createAndSaveExerciseLocation(activeMember, "Deleted Gym", true);

            flushAndClear();

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationRepository.getMembersExerciseLocations();

            // then
            assertThat(results).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("findByMemberAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_findByMemberAndExerciseLocationDeletedAtNull {

        @Nested
        @DisplayName("특정 회원이 주어졌을 때")
        class Context_with_a_specific_member {

            @Test
            @DisplayName("해당 회원의 삭제되지 않은 운동 장소 목록만 반환한다")
            void it_returns_only_active_locations_for_that_member() {
                // given
                Member member1 = createAndSaveMember("user1");
                Member member2 = createAndSaveMember("user2");

                // member1의 운동 장소들
                ExerciseLocation activeLocation1 = createAndSaveExerciseLocation(member1, "Active Gym 1", false);
                ExerciseLocation activeLocation2 = createAndSaveExerciseLocation(member1, "Active Gym 2", false);
                createAndSaveExerciseLocation(member1, "Deleted Gym", true); // 삭제된 장소

                // member2의 운동 장소 (결과에 포함되면 안 됨)
                createAndSaveExerciseLocation(member2, "Another Active Gym", false);

                flushAndClear();

                // when
                List<ExerciseLocation> results = exerciseLocationRepository.findByMemberAndExerciseLocationDeletedAtNull(member1);

                // then
                assertThat(results).hasSize(2)
                        .extracting(ExerciseLocation::getExerciseLocationName)
                        .containsExactlyInAnyOrder("Active Gym 1", "Active Gym 2");
            }

            @Test
            @DisplayName("해당 회원의 모든 운동 장소가 삭제된 상태라면 빈 리스트를 반환한다")
            void it_returns_empty_list_if_all_locations_are_deleted() {
                // given
                Member member = createAndSaveMember("userWithDeletedLocations");
                createAndSaveExerciseLocation(member, "Deleted Gym A", true);
                createAndSaveExerciseLocation(member, "Deleted Gym B", true);

                flushAndClear();

                // when
                List<ExerciseLocation> results = exerciseLocationRepository.findByMemberAndExerciseLocationDeletedAtNull(member);

                // then
                assertThat(results).isNotNull().isEmpty();
            }

            @Test
            @DisplayName("해당 회원이 운동 장소를 전혀 가지고 있지 않다면 빈 리스트를 반환한다")
            void it_returns_empty_list_if_member_has_no_locations() {
                // given
                Member member = createAndSaveMember("userWithNoLocations");
                // 이 회원은 운동 장소가 없음

                flushAndClear();

                // when
                List<ExerciseLocation> results = exerciseLocationRepository.findByMemberAndExerciseLocationDeletedAtNull(member);

                // then
                assertThat(results).isNotNull().isEmpty();
            }
        }
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}