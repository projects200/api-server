package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.record.MemberProfileAndLocationRecord;
import com.project200.undabang.member.dto.response.GetExerciseLocationsResponse;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseLocationQueryServiceImplTest {

    @InjectMocks
    private ExerciseLocationQueryServiceImpl exerciseLocationQueryService;

    @Mock
    private ExerciseLocationRepository exerciseLocationRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory();
    @Mock
    private MemberRepository memberRepository;

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

    @Nested
    @DisplayName("getExerciseLocations 메소드는")
    class Describe_getExerciseLocations {

        private final UUID memberId = UUID.randomUUID();
        private final Member member = Member.builder().memberId(memberId).memberNickname("testUser").build();

        @Test
        @DisplayName("현재 사용자의 삭제되지 않은 운동 장소 목록을 DTO 리스트로 변환하여 반환한다")
        void it_returns_list_of_dto_for_current_user_locations() {
            // given
            Point point1 = createPoint(127.1, 37.1);
            Point point2 = createPoint(127.2, 37.2);

            List<ExerciseLocation> locations = List.of(
                    ExerciseLocation.builder().exerciseLocationId(1L).exerciseLocationName("헬스장 A").exerciseLocationPoint(point1).build(),
                    ExerciseLocation.builder().exerciseLocationId(2L).exerciseLocationName("헬스장 B").exerciseLocationPoint(point2).build()
            );

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(exerciseLocationRepository.findByMemberAndExerciseLocationDeletedAtNull(member)).willReturn(locations);

                // when
                List<GetExerciseLocationsResponse> results = exerciseLocationQueryService.getExerciseLocations();

                // then
                assertThat(results).hasSize(2);
                assertThat(results)
                        .extracting("name", "latitude", "longitude")
                        .containsExactlyInAnyOrder(
                                tuple("헬스장 A", 37.1, 127.1),
                                tuple("헬스장 B", 37.2, 127.2)
                        );

                // Mock 상호작용 검증
                verify(memberRepository, times(1)).findById(memberId);
                verify(exerciseLocationRepository, times(1)).findByMemberAndExerciseLocationDeletedAtNull(member);
            }
        }

        @Test
        @DisplayName("현재 사용자의 운동 장소가 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_user_has_no_locations() {
            // given
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(exerciseLocationRepository.findByMemberAndExerciseLocationDeletedAtNull(member)).willReturn(Collections.emptyList());

                // when
                List<GetExerciseLocationsResponse> results = exerciseLocationQueryService.getExerciseLocations();

                // then
                assertThat(results).isNotNull().isEmpty();
            }
        }

        @Test
        @DisplayName("사용자 정보를 찾을 수 없으면 CustomException(MEMBER_NOT_FOUND) 예외를 던진다")
        void it_throws_exception_when_member_not_found() {
            // given
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseLocationQueryService.getExerciseLocations())
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining(ErrorCode.MEMBER_NOT_FOUND.getMessage());

                // exerciseLocationRepository는 호출되지 않아야 함
                verify(exerciseLocationRepository, never()).findByMemberAndExerciseLocationDeletedAtNull(any());
            }
        }

        private Point createPoint(double longitude, double latitude) {
            Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
            point.setSRID(4326);
            return point;
        }
    }
}