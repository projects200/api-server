package com.project200.undabang.score.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.score.dto.response.EarnablePointsInfoResponseDto;
import com.project200.undabang.score.validation.ExercisePolicyValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseScoreQueryServiceImpl 테스트")
class ExerciseScoreQueryServiceImplTest {

    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private PolicyService policyService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ExercisePolicyValidator exercisePolicyValidator;

    @InjectMocks
    private ExerciseScoreQueryServiceImpl exerciseScoreQueryService;

    private Member createMemberWithScore(byte score) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberScore(score)
                .build();
    }


    // --- 헬퍼 메서드 ---

    @Nested
    @DisplayName("getEarnablePointsInfo 메서드는")
    class Describe_getEarnablePointsInfo {

        @Nested
        @DisplayName("유효한 사용자로 호출될 때")
        class Context_with_a_valid_user {

            @Test
            @DisplayName("정확한 예상 획득 점수 정보를 계산하여 반환한다")
            void it_returns_correct_earnable_points_info() {
                // given
                UUID testUserId = UUID.randomUUID();
                Member member = createMemberWithScore((byte) 80);

                LocalDateTime endDateTime = LocalDateTime.of(2025, 7, 26, 10, 0); // 현재 시간 고정
                LocalDateTime startDateTime = endDateTime.minusDays(2); // 유효 기간 시작일
                LocalDate startDate = startDateTime.toLocalDate();
                LocalDate endDate = endDateTime.toLocalDate();

                // 하루 최대 기록 횟수는 1회, 어제(25일)는 이미 운동을 1회 한 상태로 설정
                Map<LocalDate, Long> dailyCounts = Map.of(LocalDate.of(2025, 7, 25), 1L);

                // UserContextHolder.getUserId()가 testUserId를 반환하도록 static mocking
                try (MockedStatic<UserContextHolder> holder = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(testUserId);

                    // 의존성 Mock 객체들의 동작 설정
                    given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));
                    given(policyService.getPolicyValueAsByte(PolicyKey.POINTS_PER_EXERCISE)).willReturn((byte) 3);
                    given(policyService.getPolicyValueAsByte(PolicyKey.EXERCISE_SCORE_MAX_POINTS)).willReturn((byte) 100);
                    given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_RECORD_MAX_PER_DAY)).willReturn(1);
                    given(exercisePolicyValidator.calculateValidityEndDate()).willReturn(startDateTime);
                    given(exerciseRepository.findExerciseCountsByDateBetween(member, startDate, endDate)).willReturn(dailyCounts);

                    // when
                    EarnablePointsInfoResponseDto result = exerciseScoreQueryService.getEarnablePointsInfo();

                    // then
                    assertThat(result.pointsPerExercise()).isEqualTo((byte) 3);
                    assertThat(result.currentUserScore()).isEqualTo((byte) 80);
                    assertThat(result.maxScore()).isEqualTo((byte) 100);
                    assertThat(result.validWindow().startDateTime()).isEqualTo(startDateTime);
                    // endDateTime은 now()를 사용하므로 초 단위 오차 감안하여 검증
                    assertThat(result.validWindow().endDateTime()).isAfterOrEqualTo(endDateTime);

                    // 점수 획득 가능한 날짜 검증 (24일, 26일)
                    // 25일은 이미 운동해서 제외되어야 함
                    assertThat(result.earnableScoreDates()).hasSize(2)
                            .containsExactlyInAnyOrder(
                                    LocalDate.of(2025, 7, 24),
                                    LocalDate.of(2025, 7, 26)
                            );
                }
            }
        }

        @Nested
        @DisplayName("존재하지 않는 사용자로 호출될 때")
        class Context_with_an_invalid_user {

            @Test
            @DisplayName("CustomException(MEMBER_NOT_FOUND) 예외를 발생시킨다")
            void it_throws_member_not_found_exception() {
                // given
                UUID invalidUserId = UUID.randomUUID();
                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(invalidUserId);

                    // findById가 빈 Optional을 반환하도록 설정
                    given(memberRepository.findById(invalidUserId)).willReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> exerciseScoreQueryService.getEarnablePointsInfo())
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
                }
            }
        }
    }
}