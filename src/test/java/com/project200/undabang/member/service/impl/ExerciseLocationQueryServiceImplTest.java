package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
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
        @DisplayName("레포지토리에서 받은 데이터를 그대로 반환한다")
        void it_returns_data_from_repository() {
            // given
            UUID memberId = UUID.randomUUID();
            LocalDate birthDate = LocalDate.of(1990, 1, 1);

            List<ExerciseLocationRecord> locations = List.of(
                    new ExerciseLocationRecord("헬스장A", 37.5, 127.0)
            );

            GetMembersExerciseLocationsResponse response = GetMembersExerciseLocationsResponse.builder()
                    .memberId(memberId)
                    .profileThumbnailUrl("url1")
                    .profileImageUrl("url1")
                    .nickname("user1")
                    .gender(MemberGender.MALE)
                    .birthDate(birthDate)
                    .locations(locations)
                    .build();

            List<GetMembersExerciseLocationsResponse> mockResponses = List.of(response);

            given(exerciseLocationRepository.getMembersExerciseLocations()).willReturn(mockResponses);

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

            // then
            assertThat(results).isEqualTo(mockResponses);
        }

        @Test
        @DisplayName("레포지토리에서 빈 리스트를 반환하면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_repository_returns_empty() {
            // given
            given(exerciseLocationRepository.getMembersExerciseLocations()).willReturn(List.of());

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

            // then
            assertThat(results).isEmpty();
            assertThat(results).isNotNull().isEmpty();
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
                given(exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member)).willReturn(locations);

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
                verify(exerciseLocationRepository, times(1)).findAllByMemberAndExerciseLocationDeletedAtNull(member);
            }
        }

        @Test
        @DisplayName("현재 사용자의 운동 장소가 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_user_has_no_locations() {
            // given
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member)).willReturn(Collections.emptyList());

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
                verify(exerciseLocationRepository, never()).findAllByMemberAndExerciseLocationDeletedAtNull(any());
            }
        }

        private Point createPoint(double longitude, double latitude) {
            Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
            point.setSRID(4326);
            return point;
        }
    }
}