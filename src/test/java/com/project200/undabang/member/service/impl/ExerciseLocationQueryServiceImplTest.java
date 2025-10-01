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

    @Mock
    private MemberRepository memberRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    private ExerciseLocationRecord createExerciseLocationRecord(String name, double lat, double lon) {
        return new ExerciseLocationRecord(name, lat, lon);
    }

    private GetMembersExerciseLocationsResponse createGetMembersExerciseLocationsResponse(
            UUID memberId, String thumbnailUrl, String imageUrl, String nickname,
            MemberGender gender, LocalDate birthDate, List<ExerciseLocationRecord> locations
    ) {
        return GetMembersExerciseLocationsResponse.builder()
                .memberId(memberId)
                .profileThumbnailUrl(thumbnailUrl)
                .profileImageUrl(imageUrl)
                .nickname(nickname)
                .gender(gender)
                .birthDate(birthDate)
                .locations(locations)
                .build();
    }

    private ExerciseLocation createExerciseLocation(Long id, String name, double lon, double lat) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        point.setSRID(4326);
        return ExerciseLocation.builder()
                .exerciseLocationId(id)
                .exerciseLocationName(name)
                .exerciseLocationPoint(point)
                .build();
    }

    @Nested
    @DisplayName("getMembersExerciseLocations 메소드는")
    class Describe_getMembersExerciseLocations {

        @Test
        @DisplayName("레포지토리에서 받은 데이터를 그대로 반환한다")
        void it_returns_data_from_repository() {
            // given
            UUID currentMemberId = UUID.randomUUID();
            Member currentMember = Member.builder().memberId(currentMemberId).build();
            LocalDate birthDate = LocalDate.of(1990, 1, 1);

            List<ExerciseLocationRecord> locations = List.of(
                    createExerciseLocationRecord("헬스장A", 37.5, 127.0)
            );

            GetMembersExerciseLocationsResponse response = createGetMembersExerciseLocationsResponse(
                    currentMemberId, "url1", "url1", "user1", MemberGender.MALE, birthDate, locations
            );

            List<GetMembersExerciseLocationsResponse> mockResponses = List.of(response);

            given(memberRepository.findById(currentMemberId)).willReturn(Optional.of(currentMember));
            given(exerciseLocationRepository.getMembersExerciseLocations(currentMemberId)).willReturn(mockResponses);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMemberId);

                // when
                List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

                // then
                assertThat(results).isEqualTo(mockResponses);
            }
        }

        @Test
        @DisplayName("레포지토리에서 빈 리스트를 반환하면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_repository_returns_empty() {
            // given
            UUID currentMemberId = UUID.randomUUID();
            Member currentMember = Member.builder().memberId(currentMemberId).build();
            given(memberRepository.findById(currentMemberId)).willReturn(Optional.of(currentMember));
            given(exerciseLocationRepository.getMembersExerciseLocations(currentMemberId)).willReturn(List.of());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMemberId);

                // when
                List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

                // then
                assertThat(results).isNotNull().isEmpty();
            }
        }

        @Test
        @DisplayName("현재 사용자의 memberId가 있으면 결과에서 제외된다")
        void it_excludes_current_member_from_results() {
            // given
            UUID currentMemberId = UUID.randomUUID();
            Member currentMember = Member.builder().memberId(currentMemberId).build();
            UUID otherMemberId = UUID.randomUUID();

            List<ExerciseLocationRecord> locations = List.of(
                    createExerciseLocationRecord("헬스장A", 37.5, 127.0)
            );

            GetMembersExerciseLocationsResponse otherUserResponse = createGetMembersExerciseLocationsResponse(
                    otherMemberId, "url2", "url2", "user2", MemberGender.FEMALE, LocalDate.of(1995, 5, 5), locations
            );

            given(memberRepository.findById(currentMemberId)).willReturn(Optional.of(currentMember));
            given(exerciseLocationRepository.getMembersExerciseLocations(currentMemberId)).willReturn(List.of(otherUserResponse));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMemberId);

                // when
                List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getMemberId()).isEqualTo(otherMemberId);
            }
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
            List<ExerciseLocation> locations = List.of(
                    createExerciseLocation(1L, "헬스장 A", 127.1, 37.1),
                    createExerciseLocation(2L, "헬스장 B", 127.2, 37.2)
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

                verify(exerciseLocationRepository, never()).findAllByMemberAndExerciseLocationDeletedAtNull(any());
            }
        }
    }
}