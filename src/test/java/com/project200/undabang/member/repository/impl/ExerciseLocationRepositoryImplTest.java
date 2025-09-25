package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.dto.record.MemberProfileAndLocationRecord;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

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
            createAndSaveMemberPicture(activeMember, picture); // 대표 사진 설정

            ExerciseLocation activeLocation = createAndSaveExerciseLocation(activeMember, "Active Gym", false);
            createAndSaveExerciseLocation(activeMember, "Deleted Gym", true); // 활성 유저의 삭제된 장소
            createAndSaveExerciseLocation(deletedMember, "Another Gym", false); // 삭제된 유저의 활성 장소

            flushAndClear();

            // when
            List<MemberProfileAndLocationRecord> results = exerciseLocationRepository.getMembersExerciseLocations();

            // then
            assertThat(results).hasSize(1);

            MemberProfileAndLocationRecord result = results.get(0);
            assertThat(result.memberId()).isEqualTo(activeMember.getMemberId());
            assertThat(result.nickname()).isEqualTo(activeMember.getMemberNickname());
            assertThat(result.profileThumbnailUrl()).isEqualTo(picture.getPictureUrl());
            assertThat(result.exerciseLocationName()).isEqualTo(activeLocation.getExerciseLocationName());
        }

        @Test
        @DisplayName("활성 상태의 운동 장소가 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_active_locations_exist() {
            // given
            Member activeMember = createAndSaveMember("activeUser", false);
            createAndSaveExerciseLocation(activeMember, "Deleted Gym", true); // 삭제된 장소만 존재

            flushAndClear();

            // when
            List<MemberProfileAndLocationRecord> results = exerciseLocationRepository.getMembersExerciseLocations();

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
                List<ExerciseLocation> results = exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member1);

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
                List<ExerciseLocation> results = exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member);

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
                List<ExerciseLocation> results = exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member);

                // then
                assertThat(results).isNotNull().isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("countByMemberAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_countByMemberAndExerciseLocationDeletedAtNull {

        @Nested
        @DisplayName("특정 회원이 주어졌을 때")
        class Context_with_a_specific_member {

            @Test
            @DisplayName("해당 회원의 삭제되지 않은 운동 장소의 개수만 정확히 반환한다")
            void it_returns_the_count_of_active_locations_only() {
                // given
                Member member1 = createAndSaveMember("user1");
                Member member2 = createAndSaveMember("user2");

                // user1의 장소: 활성 2개, 삭제 1개
                createAndSaveExerciseLocation(member1, "Active Gym 1", false);
                createAndSaveExerciseLocation(member1, "Active Gym 2", false);
                createAndSaveExerciseLocation(member1, "Deleted Gym", true);

                // user2의 장소: 카운트에 포함되면 안 됨
                createAndSaveExerciseLocation(member2, "Another Gym", false);

                flushAndClear();

                // when
                long count = exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member1);

                // then
                assertThat(count).isEqualTo(2L);
            }

            @Test
            @DisplayName("해당 회원의 모든 운동 장소가 삭제되었다면 0을 반환한다")
            void it_returns_zero_if_all_locations_are_deleted() {
                // given
                Member member = createAndSaveMember("user");
                createAndSaveExerciseLocation(member, "Deleted Gym A", true);
                createAndSaveExerciseLocation(member, "Deleted Gym B", true);
                flushAndClear();

                // when
                long count = exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member);

                // then
                assertThat(count).isZero();
            }

            @Test
            @DisplayName("해당 회원이 운동 장소를 가지고 있지 않다면 0을 반환한다")
            void it_returns_zero_if_member_has_no_locations() {
                // given
                Member member = createAndSaveMember("userWithNoLocations");
                flushAndClear();

                // when
                long count = exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member);

                // then
                assertThat(count).isZero();
            }
        }
    }

    @Nested
    @DisplayName("existsByExerciseLocationNameAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_existsByExerciseLocationNameAndExerciseLocationDeletedAtNull {

        @Test
        @DisplayName("삭제되지 않은 운동 장소 중 동일한 이름이 존재하면 true를 반환한다")
        void it_returns_true_when_active_location_with_same_name_exists() {
            // given
            Member member = createAndSaveMember("user");
            String existingName = "Active Gym";
            createAndSaveExerciseLocation(member, existingName, false); // 삭제되지 않은 장소
            flushAndClear();

            // when
            boolean result = exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, existingName);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("동일한 이름의 운동 장소가 존재하지만 삭제된 상태이면 false를 반환한다")
        void it_returns_false_when_location_with_same_name_is_deleted() {
            // given
            Member member = createAndSaveMember("user");
            String deletedName = "Deleted Gym";
            createAndSaveExerciseLocation(member, deletedName, true); // 삭제된 장소
            flushAndClear();

            // when
            boolean result = exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, deletedName);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("동일한 이름의 운동 장소가 존재하지 않으면 false를 반환한다")
        void it_returns_false_when_no_location_with_same_name_exists() {
            // given
            Member member = createAndSaveMember("user");
            createAndSaveExerciseLocation(member, "Some Other Gym", false);
            flushAndClear();

            // when
            boolean result = exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, "NonExistent Gym");

            // then
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

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(2000, 1, 1))
                .memberDeletedAt(null) // 테스트에서는 기본적으로 활성 유저 사용
                .build();
        em.persist(member);
        return member;
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

    private Picture createAndSavePicture(String url) {
        Picture picture = Picture.builder()
                .pictureUrl(url)
                .build();
        em.persist(picture);
        return picture;
    }

    private void createAndSaveMemberPicture(Member member, Picture picture) {
        MemberPicture memberPicture = MemberPicture.from(member, picture);
        em.persist(memberPicture);

        member.updateProfilePicture(memberPicture);
        em.persist(member);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findByExerciseLocationIdAndExerciseLocationDeletedAtNull 메소드는")
    class Describe_findByExerciseLocationIdAndExerciseLocationDeletedAtNull {

        @Test
        @DisplayName("삭제되지 않은 운동 장소를 ID로 조회하면 Optional에 담아 반환한다")
        void it_returns_location_when_exists_and_not_deleted() {
            // given
            Member member = createAndSaveMember("user");
            ExerciseLocation location = createAndSaveExerciseLocation(member, "활성 헬스장", false);
            flushAndClear();

            // when
            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(location.getExerciseLocationId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getExerciseLocationId()).isEqualTo(location.getExerciseLocationId());
            assertThat(result.get().getExerciseLocationDeletedAt()).isNull();
        }

        @Test
        @DisplayName("ID에 해당하는 운동 장소가 삭제된 경우 Optional.empty를 반환한다")
        void it_returns_empty_when_location_is_deleted() {
            // given
            Member member = createAndSaveMember("user");
            ExerciseLocation location = createAndSaveExerciseLocation(member, "삭제된 헬스장", true);
            flushAndClear();

            // when
            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(location.getExerciseLocationId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ID에 해당하는 운동 장소가 존재하지 않는 경우 Optional.empty를 반환한다")
        void it_returns_empty_when_location_does_not_exist() {
            // given
            Long nonExistentId = 999L;

            // when
            Optional<ExerciseLocation> result = exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(nonExistentId);

            // then
            assertThat(result).isEmpty();
        }
    }
}