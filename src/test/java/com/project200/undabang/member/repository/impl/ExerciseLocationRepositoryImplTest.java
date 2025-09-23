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

    // Helper Methods
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
}