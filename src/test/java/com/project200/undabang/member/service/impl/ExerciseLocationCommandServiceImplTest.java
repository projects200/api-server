package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.request.CreateExerciseLocationRequest;
import com.project200.undabang.member.dto.response.CreateExerciseLocationResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseLocationCommandServiceImplTest {

    @InjectMocks
    private ExerciseLocationCommandServiceImpl exerciseLocationCommandService;

    @Mock
    private ExerciseLocationRepository exerciseLocationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PolicyService policyService;

    @Nested
    @DisplayName("createExerciseLocation 메소드는")
    class Describe_createExerciseLocation {

        private CreateExerciseLocationRequest createExerciseLocationRequest(String name, String address, Double lat, Double lon) {
            return new CreateExerciseLocationRequest(name, address, lat, lon);
        }

        private Member createMember(UUID memberId, String nickname) {
            return Member.builder()
                    .memberId(memberId)
                    .memberNickname(nickname)
                    .build();
        }

        @Nested
        @DisplayName("유효한 생성 요청이 주어졌을 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("모든 유효성 검사를 통과하면, 운동 위치를 생성하고 응답을 반환한다")
            void it_creates_location_and_returns_response() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final Long SAVED_LOCATION_ID = 1L;
                CreateExerciseLocationRequest request = createExerciseLocationRequest("새로운 헬스장", "서울시 강남구", 37.5, 127.0);
                Member member = createMember(MEMBER_ID, "testUser");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    // [수정 전] when(exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(request.getName())).thenReturn(false);
                    // [수정 후]
                    when(exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, request.getName())).thenReturn(false);
                    when(policyService.getPolicyValueAsInt(any(PolicyKey.class))).thenReturn(10);
                    when(exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member)).thenReturn(5L);

                    when(exerciseLocationRepository.save(any(ExerciseLocation.class)))
                            .thenAnswer(invocation -> {
                                ExerciseLocation locationToSave = invocation.getArgument(0);
                                return ExerciseLocation.builder()
                                        .exerciseLocationId(SAVED_LOCATION_ID)
                                        .member(locationToSave.getMember())
                                        .exerciseLocationName(locationToSave.getExerciseLocationName())
                                        .build();
                            });

                    // when
                    CreateExerciseLocationResponse response = exerciseLocationCommandService.createExerciseLocation(request);

                    // then
                    assertThat(response).isNotNull();
                    assertThat(response.getExerciseLocationId()).isEqualTo(SAVED_LOCATION_ID);

                    verify(exerciseLocationRepository, times(1)).save(any(ExerciseLocation.class));
                }
            }
        }

        @Nested
        @DisplayName("유효하지 않은 생성 요청이 주어졌을 때")
        class Context_with_invalid_request {

            @Test
            @DisplayName("이미 존재하는 이름이면, CustomException을 던진다")
            void it_throws_exception_for_duplicate_name() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                CreateExerciseLocationRequest request = createExerciseLocationRequest("중복된 헬스장", "서울시", 37.5, 127.0);
                Member member = createMember(MEMBER_ID, "testUser");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, request.getName())).thenReturn(true);

                    assertThatThrownBy(() -> exerciseLocationCommandService.createExerciseLocation(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED);

                    verify(exerciseLocationRepository, never()).save(any());
                }
            }

            @Test
            @DisplayName("최대 생성 개수를 초과하면, CustomException을 던진다")
            void it_throws_exception_for_max_count_violation() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final int MAX_COUNT = 10;
                CreateExerciseLocationRequest request = createExerciseLocationRequest("11번째 헬스장", "서울시", 37.5, 127.0);
                Member member = createMember(MEMBER_ID, "testUser");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, request.getName())).thenReturn(false);
                    when(policyService.getPolicyValueAsInt(any(PolicyKey.class))).thenReturn(MAX_COUNT);
                    when(exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member)).thenReturn((long) MAX_COUNT);

                    assertThatThrownBy(() -> exerciseLocationCommandService.createExerciseLocation(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_MAX_COUNT_VIOLATION);

                    verify(exerciseLocationRepository, never()).save(any());
                }
            }

            @Test
            @DisplayName("회원을 찾을 수 없으면, CustomException을 던진다")
            void it_throws_exception_when_member_not_found() {
                // given
                final UUID NON_EXISTENT_MEMBER_ID = UUID.randomUUID();
                CreateExerciseLocationRequest request = createExerciseLocationRequest("아무 헬스장", "서울시", 37.5, 127.0);

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(NON_EXISTENT_MEMBER_ID);
                    when(memberRepository.findById(NON_EXISTENT_MEMBER_ID)).thenReturn(Optional.empty());

                    assertThatThrownBy(() -> exerciseLocationCommandService.createExerciseLocation(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                    verify(exerciseLocationRepository, never()).existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(any(Member.class), anyString());
                    verify(exerciseLocationRepository, never()).countByMemberAndExerciseLocationDeletedAtNull(any(Member.class));
                    verify(exerciseLocationRepository, never()).save(any());
                }
            }
        }
    }
}