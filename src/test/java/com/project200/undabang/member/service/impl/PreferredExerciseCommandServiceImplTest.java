package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.exercise.repository.ExerciseTypeRepository;
import com.project200.undabang.member.dto.request.CreatePreferredExerciseRequest;
import com.project200.undabang.member.dto.request.UpdatePreferredExerciseRequest;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.repository.PreferredExerciseRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreferredExerciseCommandServiceImpl 테스트")
class PreferredExerciseCommandServiceImplTest {

    @InjectMocks
    private PreferredExerciseCommandServiceImpl preferredExerciseCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PreferredExerciseRepository preferredExerciseRepository;

    @Mock
    private ExerciseTypeRepository exerciseTypeRepository;

    @Mock
    private PolicyService policyService;

    @Nested
    @DisplayName("createPreferredExercises 메서드는")
    class Describe_createPreferredExercises {

        @Test
        @DisplayName("유효한 요청이 주어지면 선호 운동을 생성하고 저장된 목록을 반환한다")
        void it_creates_preferred_exercises_and_returns_list() {
            // given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);

            ExerciseType exerciseType1 = createExerciseType(1L, "축구");
            ExerciseType exerciseType2 = createExerciseType(2L, "농구");

            CreatePreferredExerciseRequest request1 = createRequest(1L, ExerciseSkillLevel.BEGINNER);
            CreatePreferredExerciseRequest request2 = createRequest(2L, ExerciseSkillLevel.INTERMEDIATE);
            List<CreatePreferredExerciseRequest> requests = List.of(request1, request2);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(policyService.getPolicyValueAsInt(PolicyKey.PREFERRED_EXERCISE_MAX_COUNT)).willReturn(5);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(new ArrayList<>()); // 기존 데이터 없음
                given(exerciseTypeRepository.findAllById(anyList())).willReturn(List.of(exerciseType1, exerciseType2));

                // saveAll mocking: 입력된 리스트를 그대로 리턴 (ID는 세팅 안됨, 필요시 reflection으로 세팅 가능하지만 로직 검증엔
                // 충분)
                given(preferredExerciseRepository.saveAll(anyList())).willAnswer(invocation -> {
                    List<PreferredExercise> list = invocation.getArgument(0);
                    // ID setting for verify
                    for (int i = 0; i < list.size(); i++) {
                        ReflectionTestUtils.setField(list.get(i), "id", (long) (i + 1));
                    }
                    return list;
                });

                // when
                List<MyPreferredExerciseResponse> response = preferredExerciseCommandService
                        .createPreferredExercises(requests);

                // then
                assertThat(response).isNotNull();
                assertThat(response).hasSize(2);
                assertThat(response.get(0).getExerciseName()).isEqualTo("축구");
                assertThat(response.get(1).getExerciseName()).isEqualTo("농구");

                verify(preferredExerciseRepository, times(1)).saveAll(anyList());
            }
        }

        @Test
        @DisplayName("요청 목록이 비어있으면 빈 목록을 반환한다")
        void it_returns_empty_list_when_request_is_empty() {
            // given
            List<CreatePreferredExerciseRequest> emptyRequests = List.of();

            given(policyService.getPolicyValueAsInt(PolicyKey.PREFERRED_EXERCISE_MAX_COUNT)).willReturn(5);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(UUID.randomUUID()); // Dummy ID as
                UUID memberId = UUID.randomUUID();
                Member member = createMember(memberId);

                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(new ArrayList<>());

                // when
                List<MyPreferredExerciseResponse> response = preferredExerciseCommandService
                        .createPreferredExercises(emptyRequests);

                // then
                assertThat(response).isEmpty();
            }
        }

        @Test
        @DisplayName("기존 목록 포함 5개를 초과하면 예외를 던진다")
        void it_throws_exception_when_total_count_exceeds_max() {
            // given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);

            // 기존 3개
            List<PreferredExercise> existing = new ArrayList<>();
            existing.add(createPreferredExercise(member, createExerciseType(1L, "1")));
            existing.add(createPreferredExercise(member, createExerciseType(2L, "2")));
            existing.add(createPreferredExercise(member, createExerciseType(3L, "3")));

            // 요청 3개 -> 총 6개 (초과)
            List<CreatePreferredExerciseRequest> requests = List.of(
                    createRequest(4L, ExerciseSkillLevel.BEGINNER),
                    createRequest(5L, ExerciseSkillLevel.BEGINNER),
                    createRequest(6L, ExerciseSkillLevel.BEGINNER));

            given(policyService.getPolicyValueAsInt(PolicyKey.PREFERRED_EXERCISE_MAX_COUNT)).willReturn(5);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(existing);

                // when & then
                assertThatThrownBy(() -> preferredExerciseCommandService.createPreferredExercises(requests))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PREFERRED_EXERCISE_MAX_COUNT_VIOLATION);
            }
        }

        @Test
        @DisplayName("이미 등록된 운동 종류를 중복 추가하려하면 예외를 던진다")
        void it_throws_exception_when_duplicate_exercise_type() {
            // given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);
            ExerciseType type1 = createExerciseType(10L, "축구");

            // 기존에 '축구' 등록됨
            List<PreferredExercise> existing = List.of(createPreferredExercise(member, type1));

            // '축구' 추가 요청
            List<CreatePreferredExerciseRequest> requests = List.of(createRequest(10L, ExerciseSkillLevel.PRO));

            given(policyService.getPolicyValueAsInt(PolicyKey.PREFERRED_EXERCISE_MAX_COUNT)).willReturn(5);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(existing);

                // when & then
                assertThatThrownBy(() -> preferredExerciseCommandService.createPreferredExercises(requests))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PREFERRED_EXERCISE_DUPLICATED);
            }
        }

        @Test
        @DisplayName("요청 목록 내에 중복된 운동 종류가 있으면 예외를 던진다")
        void it_throws_exception_when_duplicate_exercise_type_in_request() {
            // given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);

            // 요청 내 중복 (10L 두 번)
            List<CreatePreferredExerciseRequest> requests = List.of(
                    createRequest(10L, ExerciseSkillLevel.PRO),
                    createRequest(10L, ExerciseSkillLevel.BEGINNER));

            given(policyService.getPolicyValueAsInt(PolicyKey.PREFERRED_EXERCISE_MAX_COUNT)).willReturn(5);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(new ArrayList<>());

                // when & then
                assertThatThrownBy(() -> preferredExerciseCommandService.createPreferredExercises(requests))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PREFERRED_EXERCISE_DUPLICATED_IN_REQUEST);
            }
        }

        @Test
        @DisplayName("존재하지 않는 운동 종류 ID가 포함되면 예외를 던진다")
        void it_throws_exception_when_exercise_type_not_found() {
            // given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);

            List<CreatePreferredExerciseRequest> requests = List.of(createRequest(999L, ExerciseSkillLevel.BEGINNER));

            given(policyService.getPolicyValueAsInt(PolicyKey.PREFERRED_EXERCISE_MAX_COUNT)).willReturn(5);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(new ArrayList<>());

                // 조회 결과 없음
                given(exerciseTypeRepository.findAllById(anyList())).willReturn(List.of());

                // when & then
                assertThatThrownBy(() -> preferredExerciseCommandService.createPreferredExercises(requests))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PREFERRED_EXERCISE_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("deletePreferredExercises 메서드는")
    class Describe_deletePreferredExercises {

        @Test
        @DisplayName("유효한 ID 목록이 주어지면 선호 운동을 삭제한다")
        void it_deletes_preferred_exercises_successfully() {
            // given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);

            ExerciseType type1 = createExerciseType(1L, "축구");
            ExerciseType type2 = createExerciseType(2L, "농구");

            PreferredExercise exercise1 = createPreferredExercise(member, type1);
            PreferredExercise exercise2 = createPreferredExercise(member, type2);
            ReflectionTestUtils.setField(exercise1, "id", 100L);
            ReflectionTestUtils.setField(exercise2, "id", 101L);

            List<Long> deleteIds = List.of(100L, 101L);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByIdInAndMemberAndPreferredExerciseDeletedAtNull(deleteIds,
                        member))
                        .willReturn(List.of(exercise1, exercise2));

                // when
                preferredExerciseCommandService.deletePreferredExercises(deleteIds);

                // then
                // soft delete 확인 (deletedAt 필드가 설정되었는지)
                assertThat(exercise1.getPreferredExerciseDeletedAt()).isNotNull();
                assertThat(exercise2.getPreferredExerciseDeletedAt()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("updatePreferredExercises 메서드는")
    class Describe_updatePreferredExercises {

        @Test
        @DisplayName("유효한 요청이 주어지면 선호 운동 정보를 수정하고 결과 목록을 반환한다")
        void it_updates_preferred_exercises_successfully() {
            // given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);

            // 기존: 축구(BEGINNER), 농구(BEGINNER)
            ExerciseType type1 = createExerciseType(10L, "축구");
            ExerciseType type2 = createExerciseType(11L, "농구");
            PreferredExercise exercise1 = createPreferredExercise(member, type1);
            PreferredExercise exercise2 = createPreferredExercise(member, type2);

            List<PreferredExercise> existing = List.of(exercise1, exercise2);

            // 요청: 축구(PRO), 농구(INTERMEDIATE)로 변경
            UpdatePreferredExerciseRequest req1 = createUpdateRequest(10L, ExerciseSkillLevel.PRO);
            UpdatePreferredExerciseRequest req2 = createUpdateRequest(11L, ExerciseSkillLevel.INTERMEDIATE);
            List<UpdatePreferredExerciseRequest> requests = List.of(req1, req2);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(existing);

                // when
                List<MyPreferredExerciseResponse> response = preferredExerciseCommandService
                        .updatePreferredExercises(requests);

                // then
                assertThat(response).hasSize(2);

                // 순서는 보장되지 않을 수 있으므로, ID나 이름 등으로 매칭하여 검증
                MyPreferredExerciseResponse res1 = response.stream()
                        .filter(r -> r.getExerciseTypeId().equals(10L)).findFirst().orElseThrow();
                assertThat(res1.getSkillLevel()).isEqualTo(ExerciseSkillLevel.PRO);

                MyPreferredExerciseResponse res2 = response.stream()
                        .filter(r -> r.getExerciseTypeId().equals(11L)).findFirst().orElseThrow();
                assertThat(res2.getSkillLevel()).isEqualTo(ExerciseSkillLevel.INTERMEDIATE);

                // entity 상태 변화 검증
                assertThat(exercise1.getPreferredExerciseSkillLevel()).isEqualTo(ExerciseSkillLevel.PRO);
                assertThat(exercise2.getPreferredExerciseSkillLevel()).isEqualTo(ExerciseSkillLevel.INTERMEDIATE);
            }
        }

        @Test
        @DisplayName("보유하지 않은 운동을 수정하려 하면 예외를 던진다")
        void it_throws_exception_when_updating_not_owned_exercise() {
            // given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);

            // 기존: 축구(10L)만 보유
            ExerciseType type1 = createExerciseType(10L, "축구");
            PreferredExercise exercise1 = createPreferredExercise(member, type1);
            List<PreferredExercise> existing = List.of(exercise1);

            // 요청: 농구(20L) 수정 시도
            UpdatePreferredExerciseRequest req = createUpdateRequest(20L, ExerciseSkillLevel.PRO);
            List<UpdatePreferredExerciseRequest> requests = List.of(req);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(existing);

                // when & then
                assertThatThrownBy(() -> preferredExerciseCommandService.updatePreferredExercises(requests))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PREFERRED_EXERCISE_NOT_FOUND);
            }
        }
    }

    private Member createMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberEmail("test@test.com")
                .memberNickname("test")
                .memberGender(MemberGender.FEMALE)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private ExerciseType createExerciseType(Long id, String name) {
        ExerciseType type = ExerciseType.builder()
                .exerciseName(name)
                .exerciseTypeImageUrl("url")
                .build();
        ReflectionTestUtils.setField(type, "id", id);
        return type;
    }

    private PreferredExercise createPreferredExercise(Member member, ExerciseType type) {
        return PreferredExercise.builder()
                .member(member)
                .exercise(type)
                .preferredExerciseSkillLevel(ExerciseSkillLevel.BEGINNER)
                .build();
    }

    private CreatePreferredExerciseRequest createRequest(Long exerciseTypeId, ExerciseSkillLevel level) {
        CreatePreferredExerciseRequest request = new CreatePreferredExerciseRequest();
        ReflectionTestUtils.setField(request, "exerciseTypeId", exerciseTypeId);
        ReflectionTestUtils.setField(request, "skillLevel", level);
        ReflectionTestUtils.setField(request, "daysOfWeek", new boolean[7]);
        return request;
    }

    private UpdatePreferredExerciseRequest createUpdateRequest(Long exerciseTypeId, ExerciseSkillLevel level) {
        UpdatePreferredExerciseRequest request = new UpdatePreferredExerciseRequest();
        ReflectionTestUtils.setField(request, "exerciseTypeId", exerciseTypeId);
        ReflectionTestUtils.setField(request, "skillLevel", level);
        ReflectionTestUtils.setField(request, "daysOfWeek",
                new boolean[] { true, false, true, false, true, false, false });
        return request;
    }
}
