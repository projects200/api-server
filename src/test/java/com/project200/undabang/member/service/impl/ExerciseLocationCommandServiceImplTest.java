package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.request.CreateExerciseLocationRequest;
import com.project200.undabang.member.dto.request.UpdateExerciseLocationRequest;
import com.project200.undabang.member.dto.response.CreateExerciseLocationResponse;
import com.project200.undabang.member.dto.response.UpdateExerciseLocationResponse;
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

    private CreateExerciseLocationRequest createExerciseLocationRequest(String name, String address, Double lat, Double lon) {
        return new CreateExerciseLocationRequest(name, address, lat, lon);
    }

    private UpdateExerciseLocationRequest updateRequest(String newName) {
        return new UpdateExerciseLocationRequest(newName);
    }

    private Member createMember(UUID memberId, String nickname) {
        return Member.builder()
                .memberId(memberId)
                .memberNickname(nickname)
                .build();
    }

    private ExerciseLocation createExerciseLocation(Long locationId, Member member, String name) {
        return ExerciseLocation.builder()
                .exerciseLocationId(locationId)
                .member(member)
                .exerciseLocationName(name)
                .build();
    }

    @Nested
    @DisplayName("createExerciseLocation 메소드는")
    class Describe_createExerciseLocation {

        @Nested
        @DisplayName("유효한 생성 요청이 주어지면")
        class Context_with_valid_request {

            @Test
            @DisplayName("운동 위치를 생성하고 생성된 위치의 응답을 반환한다")
            void it_creates_location_and_returns_response() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final Long SAVED_LOCATION_ID = 1L;
                CreateExerciseLocationRequest request = createExerciseLocationRequest("새로운 헬스장", "서울시 강남구", 37.5, 127.0);
                Member member = createMember(MEMBER_ID, "testUser");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
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
        @DisplayName("이미 존재하는 이름으로 요청하면")
        class Context_with_duplicate_name {

            @Test
            @DisplayName("CustomException(NAME_DUPLICATED)을 던진다")
            void it_throws_exception_for_duplicate_name() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                CreateExerciseLocationRequest request = createExerciseLocationRequest("중복된 헬스장", "서울시", 37.5, 127.0);
                Member member = createMember(MEMBER_ID, "testUser");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, request.getName())).thenReturn(true);

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.createExerciseLocation(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED);

                    verify(exerciseLocationRepository, never()).save(any());
                }
            }
        }

        @Nested
        @DisplayName("최대 생성 개수를 초과하면")
        class Context_with_max_count_violation {

            @Test
            @DisplayName("CustomException(MAX_COUNT_VIOLATION)을 던진다")
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

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.createExerciseLocation(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_MAX_COUNT_VIOLATION);

                    verify(exerciseLocationRepository, never()).save(any());
                }
            }
        }

        @Nested
        @DisplayName("요청한 회원을 찾을 수 없으면")
        class Context_with_member_not_found {

            @Test
            @DisplayName("CustomException(MEMBER_NOT_FOUND)을 던진다")
            void it_throws_exception_when_member_not_found() {
                // given
                final UUID NON_EXISTENT_MEMBER_ID = UUID.randomUUID();
                CreateExerciseLocationRequest request = createExerciseLocationRequest("아무 헬스장", "서울시", 37.5, 127.0);

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(NON_EXISTENT_MEMBER_ID);
                    when(memberRepository.findById(NON_EXISTENT_MEMBER_ID)).thenReturn(Optional.empty());

                    // when & then
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

    @Nested
    @DisplayName("updateExerciseLocation 메소드는")
    class Describe_updateExerciseLocation {

        @Nested
        @DisplayName("해당 회원의 삭제되지 않은 운동장소가 존재하고 이름이 변경되었다면")
        class Context_with_valid_request_and_name_change {

            @Test
            @DisplayName("이름을 수정하고 업데이트된 응답을 반환한다")
            void it_updates_location_name_and_returns_response() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final Long LOCATION_ID = 1L;
                final String OLD_NAME = "기존 헬스장";
                final String NEW_NAME = "수정된 헬스장";
                Member member = createMember(MEMBER_ID, "testUser");
                ExerciseLocation location = createExerciseLocation(LOCATION_ID, member, OLD_NAME);

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(LOCATION_ID))
                            .thenReturn(Optional.of(location));
                    when(exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, NEW_NAME))
                            .thenReturn(false);

                    UpdateExerciseLocationRequest request = updateRequest(NEW_NAME);

                    // when
                    UpdateExerciseLocationResponse response = exerciseLocationCommandService.updateExerciseLocation(LOCATION_ID, request);

                    // then
                    assertThat(response).isNotNull();
                    assertThat(response.getId()).isEqualTo(LOCATION_ID);
                    assertThat(location.getExerciseLocationName()).isEqualTo(NEW_NAME);
                }
            }
        }

        @Nested
        @DisplayName("이름이 변경되지 않았다면")
        class Context_with_unchanged_name {

            @Test
            @DisplayName("중복 검사를 수행하지 않고 성공 응답을 반환한다")
            void it_does_not_validate_duplicate_when_name_is_unchanged() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final Long LOCATION_ID = 1L;
                final String SAME_NAME = "기존 헬스장";
                Member member = createMember(MEMBER_ID, "testUser");
                ExerciseLocation location = createExerciseLocation(LOCATION_ID, member, SAME_NAME);

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(LOCATION_ID))
                            .thenReturn(Optional.of(location));

                    UpdateExerciseLocationRequest request = updateRequest(SAME_NAME);

                    // when
                    UpdateExerciseLocationResponse response = exerciseLocationCommandService.updateExerciseLocation(LOCATION_ID, request);

                    // then
                    assertThat(response).isNotNull();
                    assertThat(response.getId()).isEqualTo(LOCATION_ID);
                    assertThat(location.getExerciseLocationName()).isEqualTo(SAME_NAME);

                    verify(exerciseLocationRepository, never()).existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(any(Member.class), anyString());
                }
            }
        }

        @Nested
        @DisplayName("운동장소가 없거나 이미 삭제된 경우")
        class Context_with_non_existent_location {

            @Test
            @DisplayName("CustomException(NOT_FOUND)을 던진다")
            void it_throws_not_found_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final Long NON_EXISTENT_LOCATION_ID = 999L;
                Member member = createMember(MEMBER_ID, "testUser");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(NON_EXISTENT_LOCATION_ID))
                            .thenReturn(Optional.empty());

                    UpdateExerciseLocationRequest request = updateRequest("새 이름");

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.updateExerciseLocation(NON_EXISTENT_LOCATION_ID, request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_NOT_FOUND);
                }
            }
        }

        @Nested
        @DisplayName("자신이 소유한 운동장소가 아니면")
        class Context_with_other_member_location {

            @Test
            @DisplayName("CustomException(AUTHORIZATION_DENIED)을 던진다")
            void it_throws_authorization_denied_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final UUID OTHER_MEMBER_ID = UUID.randomUUID();
                final Long LOCATION_ID = 1L;
                Member member = createMember(MEMBER_ID, "testUser");
                Member otherMember = createMember(OTHER_MEMBER_ID, "otherUser");
                ExerciseLocation locationOfOtherMember = createExerciseLocation(LOCATION_ID, otherMember, "다른 사람의 헬스장");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(LOCATION_ID))
                            .thenReturn(Optional.of(locationOfOtherMember));

                    UpdateExerciseLocationRequest request = updateRequest("새 이름");

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.updateExerciseLocation(LOCATION_ID, request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);
                }
            }
        }

        @Nested
        @DisplayName("수정하려는 이름이 이미 존재하면")
        class Context_with_duplicate_new_name {

            @Test
            @DisplayName("CustomException(NAME_DUPLICATED)을 던진다")
            void it_throws_name_duplicated_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final Long LOCATION_ID = 1L;
                final String OLD_NAME = "기존 헬스장";
                final String DUPLICATE_NAME = "이미 있는 헬스장";
                Member member = createMember(MEMBER_ID, "testUser");
                ExerciseLocation location = createExerciseLocation(LOCATION_ID, member, OLD_NAME);

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(LOCATION_ID))
                            .thenReturn(Optional.of(location));
                    when(exerciseLocationRepository.existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(member, DUPLICATE_NAME))
                            .thenReturn(true);

                    UpdateExerciseLocationRequest request = updateRequest(DUPLICATE_NAME);

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.updateExerciseLocation(LOCATION_ID, request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED);
                }
            }
        }
    }

    @Nested
    @DisplayName("deleteExerciseLocation 메소드는")
    class Describe_deleteExerciseLocation {

        @Nested
        @DisplayName("존재하는 본인의 운동장소 ID가 주어지면")
        class Context_with_valid_id {

            @Test
            @DisplayName("해당 운동장소를 성공적으로 삭제 처리한다")
            void it_soft_deletes_the_exercise_location() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final Long LOCATION_ID = 1L;
                Member member = createMember(MEMBER_ID, "testUser");
                ExerciseLocation location = createExerciseLocation(LOCATION_ID, member, "삭제할 헬스장");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(LOCATION_ID))
                            .thenReturn(Optional.of(location));

                    // when
                    exerciseLocationCommandService.deleteExerciseLocation(LOCATION_ID);

                    // then
                    verify(exerciseLocationRepository, times(1)).findByExerciseLocationIdAndExerciseLocationDeletedAtNull(LOCATION_ID);
                    // 실제 엔티티의 삭제 메서드가 호출되었는지 확인하려면 spy 등을 쓸 수 있으나,
                    // 여기서는 repository 호출 여부와 에러가 발생하지 않음을 확인
                }
            }
        }

        @Nested
        @DisplayName("삭제할 운동장소가 존재하지 않으면")
        class Context_with_non_existent_location {

            @Test
            @DisplayName("CustomException(NOT_FOUND)을 던진다")
            void it_throws_not_found_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final Long NON_EXISTENT_LOCATION_ID = 999L;
                Member member = createMember(MEMBER_ID, "testUser");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(NON_EXISTENT_LOCATION_ID))
                            .thenReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.deleteExerciseLocation(NON_EXISTENT_LOCATION_ID))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_NOT_FOUND);
                }
            }
        }

        @Nested
        @DisplayName("자신이 소유한 운동장소가 아니면")
        class Context_with_other_member_location {

            @Test
            @DisplayName("CustomException(AUTHORIZATION_DENIED)을 던진다")
            void it_throws_authorization_denied_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final UUID OTHER_MEMBER_ID = UUID.randomUUID();
                final Long LOCATION_ID = 1L;
                Member member = createMember(MEMBER_ID, "testUser");
                Member otherMember = createMember(OTHER_MEMBER_ID, "otherUser");
                ExerciseLocation locationOfOtherMember = createExerciseLocation(LOCATION_ID, otherMember, "다른 사람의 헬스장");

                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.findByExerciseLocationIdAndExerciseLocationDeletedAtNull(LOCATION_ID))
                            .thenReturn(Optional.of(locationOfOtherMember));

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.deleteExerciseLocation(LOCATION_ID))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);
                }
            }
        }
    }
}
