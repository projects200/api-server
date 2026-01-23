package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.record.Viewport;
import com.project200.undabang.member.dto.response.GetExerciseLocationsResponse;
import com.project200.undabang.member.dto.response.GetOtherMemberExerciseLocationsResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.repository.MemberBlockRepository;
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

import java.util.*;

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

    @Mock
    private MemberBlockRepository memberBlockRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory();

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
                    createExerciseLocation(1L, "헬스장 A", 37.1, 127.1),
                    createExerciseLocation(2L, "헬스장 B", 37.2, 127.2));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member))
                        .willReturn(locations);

                // when
                List<GetExerciseLocationsResponse> results = exerciseLocationQueryService.getExerciseLocations();

                // then
                assertThat(results).hasSize(2);
                assertThat(results)
                        .extracting("name", "latitude", "longitude")
                        .containsExactlyInAnyOrder(
                                tuple("헬스장 A", 127.1, 37.1),
                                tuple("헬스장 B", 127.2, 37.2));

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

    @Nested
    @DisplayName("getMembersExerciseLocations 메소드는")
    class Describe_getMembersExerciseLocations {

        @Test
        @DisplayName("차단 목록을 조회하고, 이를 기반으로 주변 회원 목록을 조회하여 반환한다")
        void it_finds_exclusion_list_and_then_finds_nearby_members() {
            try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                // given
                UUID currentUserId = UUID.randomUUID();
                Member currentUser = Member.builder().memberId(currentUserId).build();
                mockedUserContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

                UUID otherUser1Id = UUID.randomUUID();
                UUID otherUser2Id = UUID.randomUUID(); // 차단된 유저

                // 1. 차단 목록 조회 Mocking
                Set<UUID> exclusionIds = Set.of(currentUserId, otherUser2Id);
                given(memberRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
                given(memberBlockRepository.findAllBlockedMemberIdsByMember(currentUser)).willReturn(exclusionIds);

                // 2. 주변 회원 목록 조회 Mocking
                Set<ExerciseLocationRecord> locations = Set.of(createExerciseLocationRecord(1L, "헬스장A", 37.5, 127.0));
                GetOtherMemberExerciseLocationsResponse response1 = createGetMembersExerciseLocationsResponse(otherUser1Id,
                        "user1", locations);
                List<GetOtherMemberExerciseLocationsResponse> finalResponse = List.of(response1);
                given(exerciseLocationRepository.getMembersExerciseLocations(
                        eq(exclusionIds), any(Viewport.class)))
                        .willReturn(finalResponse);

                // when
                List<GetOtherMemberExerciseLocationsResponse> results = exerciseLocationQueryService
                        .getMembersExerciseLocations(
                                new Viewport(37.0, 127.0, 36.0, 128.0));

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getMemberId()).isEqualTo(otherUser1Id);

                // verify: 각 Mock 객체가 올바른 순서와 파라미터로 호출되었는지 검증
                verify(memberRepository, times(1)).findById(currentUserId);
                verify(memberBlockRepository, times(1)).findAllBlockedMemberIdsByMember(currentUser);
                verify(exerciseLocationRepository, times(1)).getMembersExerciseLocations(
                        eq(exclusionIds), any(Viewport.class));
            }
        }

        @Test
        @DisplayName("사용자 정보를 찾을 수 없으면 CustomException(MEMBER_NOT_FOUND) 예외를 던진다")
        void it_throws_exception_when_member_not_found() {
            try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                // given
                UUID currentUserId = UUID.randomUUID();
                mockedUserContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
                given(memberRepository.findById(currentUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseLocationQueryService.getMembersExerciseLocations(
                        new Viewport(37.0, 127.0, 36.0, 128.0)))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining(ErrorCode.MEMBER_NOT_FOUND.getMessage());

                // verify: 예외 발생 시 다른 리포지토리들은 호출되지 않아야 함
                verify(memberBlockRepository, never()).findAllBlockedMemberIdsByMember(any());
                verify(exerciseLocationRepository, never()).getMembersExerciseLocations(any(), any(Viewport.class));
            }
        }
    }

    private GetOtherMemberExerciseLocationsResponse createGetMembersExerciseLocationsResponse(
            UUID memberId, String nickname, Set<ExerciseLocationRecord> locations) {
        return GetOtherMemberExerciseLocationsResponse.builder()
                .memberId(memberId)
                .nickname(nickname)
                .locations(locations)
                .build();
    }

    private ExerciseLocation createExerciseLocation(Long id, String name, double lat, double lon) {
        Point point = geometryFactory.createPoint(new Coordinate(lat, lon));
        point.setSRID(4326);
        return ExerciseLocation.builder()
                .exerciseLocationId(id)
                .exerciseLocationName(name)
                .exerciseLocationPoint(point)
                .build();
    }

    private ExerciseLocationRecord createExerciseLocationRecord(Long id, String name, double lat, double lon) {
        return new ExerciseLocationRecord(id, name, lat, lon);
    }
}