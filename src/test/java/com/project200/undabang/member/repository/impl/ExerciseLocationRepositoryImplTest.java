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

    private final GeometryFactory geometryFactory = new GeometryFactory();
    @Autowired
    private EntityManager em;
    @Autowired
    private ExerciseLocationRepository exerciseLocationRepository;

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
}