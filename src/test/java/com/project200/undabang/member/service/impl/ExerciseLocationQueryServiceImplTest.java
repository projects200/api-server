package com.project200.undabang.member.service.impl;

import com.project200.undabang.member.dto.record.MemberProfileAndLocationRecord;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ExerciseLocationQueryServiceImplTest {

    private final GeometryFactory geometryFactory = new GeometryFactory();
    @InjectMocks
    private ExerciseLocationQueryServiceImpl exerciseLocationQueryService;
    @Mock
    private ExerciseLocationRepository exerciseLocationRepository;

    @Nested
    @DisplayName("getMembersExerciseLocations 메소드는")
    class Describe_getMembersExerciseLocations {

        @Test
        @DisplayName("레포지토리에서 받은 데이터를 회원 ID별로 그룹화하여 반환한다")
        void it_groups_data_by_member_id_and_returns() {
            // given
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();
            LocalDate birthDate1 = LocalDate.of(1990, 1, 1);
            LocalDate birthDate2 = LocalDate.of(1995, 5, 5);

            List<MemberProfileAndLocationRecord> mockRecords = List.of(
                    new MemberProfileAndLocationRecord(memberId1, "user1", MemberGender.MALE, birthDate1, "url1", "Gym A", createPoint(127.0, 37.5)),
                    new MemberProfileAndLocationRecord(memberId1, "user1", MemberGender.MALE, birthDate1, "url1", "Gym B", createPoint(127.1, 37.6)),
                    new MemberProfileAndLocationRecord(memberId2, "user2", MemberGender.FEMALE, birthDate2, "url2", "Gym C", createPoint(127.2, 37.7))
            );

            given(exerciseLocationRepository.getMembersExerciseLocations()).willReturn(mockRecords);

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

            // then
            assertThat(results).hasSize(2);

            // 첫 번째 회원(member1) 검증
            GetMembersExerciseLocationsResponse response1 = results.stream()
                    .filter(r -> r.getMemberId().equals(memberId1))
                    .findFirst()
                    .orElse(null);

            assertThat(response1).isNotNull();
            assertThat(response1.getNickname()).isEqualTo("user1");
            assertThat(response1.getProfileImageUrl()).isEqualTo("url1");
            assertThat(response1.getGender()).isEqualTo(MemberGender.MALE);
            assertThat(response1.getBirthDate()).isEqualTo(birthDate1);
            assertThat(response1.getLocations()).hasSize(2);
            assertThat(response1.getLocations())
                    .extracting("exerciseLocationName")
                    .containsExactlyInAnyOrder("Gym A", "Gym B");
            assertThat(response1.getLocations())
                    .extracting("latitude")
                    .containsExactlyInAnyOrder(37.5, 37.6);

            // 두 번째 회원(member2) 검증
            GetMembersExerciseLocationsResponse response2 = results.stream()
                    .filter(r -> r.getMemberId().equals(memberId2))
                    .findFirst()
                    .orElse(null);

            assertThat(response2).isNotNull();
            assertThat(response2.getNickname()).isEqualTo("user2");
            assertThat(response2.getProfileImageUrl()).isEqualTo("url2");
            assertThat(response2.getGender()).isEqualTo(MemberGender.FEMALE);
            assertThat(response2.getBirthDate()).isEqualTo(birthDate2);
            assertThat(response2.getLocations()).hasSize(1);
            assertThat(response2.getLocations().get(0).exerciseLocationName()).isEqualTo("Gym C");
            assertThat(response2.getLocations().get(0).latitude()).isEqualTo(37.7);
            assertThat(response2.getLocations().get(0).longitude()).isEqualTo(127.2);
        }

        @Test
        @DisplayName("활성 운동 장소가 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_active_locations() {
            // given
            given(exerciseLocationRepository.getMembersExerciseLocations()).willReturn(List.of());

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

            // then
            assertThat(results).isNotNull().isEmpty();
        }

        private Point createPoint(double longitude, double latitude) {
            Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
            point.setSRID(4326);
            return point;
        }
    }
}