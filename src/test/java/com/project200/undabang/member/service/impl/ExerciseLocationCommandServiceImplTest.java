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

                // UserContextHolder.getUserId()는 static 메소드이므로, MockedStatic을 사용합니다.
                try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
                    // Mock 설정
                    mockedUserContext.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
                    when(exerciseLocationRepository.existsByExerciseLocationNameAndExerciseLocationDeletedAtNull(request.getName())).thenReturn(false);
                    when(policyService.getPolicyValueAsInt(any(PolicyKey.class))).thenReturn(10); // 최대 10개
                    when(exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member)).thenReturn(5L); // 현재 5개

                    // save 메소드가 호출되면 ID가 부여된 엔티티를 반환하도록 설정
                    when(exerciseLocationRepository.save(any(ExerciseLocation.class)))
                            .thenAnswer(invocation -> {
                                ExerciseLocation locationToSave = invocation.getArgument(0);
                                // 실제 DB처럼 ID를 설정해주는 것을 흉내 냅니다.
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

                    // verify: save 메소드가 정확히 1번 호출되었는지 검증
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
                    // 이름 중복 검사에서 true를 반환하도록 설정
                    when(exerciseLocationRepository.existsByExerciseLocationNameAndExerciseLocationDeletedAtNull(request.getName())).thenReturn(true);

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.createExerciseLocation(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED);

                    // verify: 예외가 발생했으므로, save 메소드는 절대 호출되면 안 됨
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
                    // 이름 중복은 통과
                    when(exerciseLocationRepository.existsByExerciseLocationNameAndExerciseLocationDeletedAtNull(request.getName())).thenReturn(false);
                    // 정책상 최대 개수는 10개
                    when(policyService.getPolicyValueAsInt(any(PolicyKey.class))).thenReturn(MAX_COUNT);
                    // 현재 회원은 이미 10개를 가지고 있음
                    when(exerciseLocationRepository.countByMemberAndExerciseLocationDeletedAtNull(member)).thenReturn((long) MAX_COUNT);

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.createExerciseLocation(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_LOCATION_MAX_COUNT_VIOLATION);

                    // verify
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
                    // 회원을 찾지 못해 Optional.empty()를 반환하도록 설정
                    when(memberRepository.findById(NON_EXISTENT_MEMBER_ID)).thenReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> exerciseLocationCommandService.createExerciseLocation(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                    // verify: 회원 조회 단계에서 실패했으므로 운동 위치 관련 repository는 호출되면 안 됨
                    verify(exerciseLocationRepository, never()).existsByExerciseLocationNameAndExerciseLocationDeletedAtNull(anyString());
                    verify(exerciseLocationRepository, never()).countByMemberAndExerciseLocationDeletedAtNull(any(Member.class));
                    verify(exerciseLocationRepository, never()).save(any());
                }
            }
        }
    }
}