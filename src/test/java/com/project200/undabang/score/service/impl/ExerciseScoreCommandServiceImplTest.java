package com.project200.undabang.score.service.impl;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.MemberScoreErrorDto;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.score.validation.ExercisePolicyValidator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseScoreCommandServiceImpl 테스트")
class ExerciseScoreCommandServiceImplTest {

    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private PolicyService policyService;
    @Mock
    private ExercisePolicyValidator exercisePolicyValidator;
    @Mock
    private NotifyErrorToAdmin notifyErrorToAdmin;

    @InjectMocks
    private ExerciseScoreCommandServiceImpl exerciseScoreCommandService;

    /**
     * 점수 획득 성공 시나리오에 필요한 모든 Mock 객체의 기본 동작을 설정합니다.
     */
    private void setupSuccessConditions() {
        // 유효 기간 검증 통과
        given(exercisePolicyValidator.calculateValidityEndDate()).willReturn(LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.DAYS));

        // 일일 최대 기록 횟수 통과
        given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_RECORD_MAX_PER_DAY)).willReturn(2);
        given(exerciseRepository.countByMemberAndExerciseStartedAt(any(), any())).willReturn(0L);

        // 점수 정책 설정
        given(policyService.getPolicyValueAsByte(PolicyKey.POINTS_PER_EXERCISE)).willReturn((byte) 3);
        given(policyService.getPolicyValueAsByte(PolicyKey.EXERCISE_SCORE_MAX_POINTS)).willReturn((byte) 100);
        given(policyService.getPolicyValueAsByte(PolicyKey.EXERCISE_SCORE_MIN_POINTS)).willReturn((byte) 0);
    }


    // --- 헬퍼 메서드: 테스트 데이터 생성을 위한 재사용 로직 ---

    private Member createMemberWithScore(byte score) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberScore(score)
                .build();
    }

    private Exercise createExerciseFor(Member member, LocalDateTime startedAt) {
        return Exercise.builder()
                .id(1L)
                .member(member)
                .exerciseStartedAt(startedAt)
                .build();
    }

    @Nested
    @DisplayName("awardPointsForExercise 메서드는")
    class Describe_awardPointsForExercise {

        @Nested
        @DisplayName("점수 획득 조건을 모두 만족하는 경우")
        class Context_with_all_conditions_met {

            @ParameterizedTest
            @CsvSource({
                    "50, 3, 53", // 시나리오 1: 초기점수 50, 기대 획득점수 3, 최종점수 53
                    "99, 1, 100"  // 시나리오 2: 초기점수 99, 기대 획득점수 1, 최종점수 100
            })
            @DisplayName("계산된 점수를 부여하고 실제 획득한 점수를 반환한다")
            void it_awards_points_and_returns_earned_points(byte initialScore, byte expectedEarnedPoints, byte finalScore) {
                // given
                Member member = createMemberWithScore(initialScore);
                Exercise exercise = createExerciseFor(member, LocalDateTime.now());

                setupSuccessConditions();

                // when
                byte actualEarnedPoints = exerciseScoreCommandService.awardPointsForExercise(exercise);

                // then
                assertThat(actualEarnedPoints).isEqualTo(expectedEarnedPoints);
                assertThat(member.getMemberScore()).isEqualTo(finalScore);
            }
        }

        @Nested
        @DisplayName("점수 획득 조건을 만족하지 못하는 경우")
        class Context_with_conditions_not_met {

            @Test
            @DisplayName("유효 기간을 벗어났으면 0점을 반환한다")
            void it_returns_zero_if_outside_validity_period() {
                // given
                Member member = createMemberWithScore((byte) 50);
                LocalDateTime exerciseTime = LocalDateTime.now();
                Exercise exercise = createExerciseFor(member, exerciseTime);

                // 유효 기간 검증 실패 설정 (운동 시작 시간이 유효 기간 종료일보다 이름)
                given(exercisePolicyValidator.calculateValidityEndDate()).willReturn(exerciseTime.plusDays(1));

                // when
                byte earnedPoints = exerciseScoreCommandService.awardPointsForExercise(exercise);

                // then
                assertThat(earnedPoints).isZero();
                assertThat(member.getMemberScore()).isEqualTo((byte) 50); // 점수 변동 없음
            }

            @Test
            @DisplayName("일일 최대 기록 횟수를 초과했으면 0점을 반환한다")
            void it_returns_zero_if_max_records_per_day_exceeded() {
                // given
                Member member = createMemberWithScore((byte) 50);
                Exercise exercise = createExerciseFor(member, LocalDateTime.now());

                // 유효 기간은 통과
                given(exercisePolicyValidator.calculateValidityEndDate()).willReturn(LocalDateTime.now().minusDays(1));

                // 일일 최대 기록 횟수 초과 설정
                given(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_RECORD_MAX_PER_DAY)).willReturn(1);
                given(exerciseRepository.countByMemberAndExerciseStartedAt(any(Member.class), any())).willReturn(1L);

                // when
                byte earnedPoints = exerciseScoreCommandService.awardPointsForExercise(exercise);

                // then
                assertThat(earnedPoints).isZero();
                assertThat(member.getMemberScore()).isEqualTo((byte) 50); // 점수 변동 없음
            }
        }

        @Nested
        @DisplayName("로직 수행 중 예외가 발생하는 경우")
        class Context_with_exception {

            @Test
            @DisplayName("0점을 반환하고 회원의 점수는 변하지 않는다")
            void it_returns_zero_and_member_score_is_unchanged() {
                setUpExceptionTestContext("/api/v1/exercise/award", "POST");
                try(MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)){
                    // given
                    Member member = createMemberWithScore((byte) 50);
                    Exercise exercise = createExerciseFor(member, LocalDateTime.now());

                    // PolicyService 호출 시 의도적으로 예외 발생
                    given(exercisePolicyValidator.calculateValidityEndDate()).willThrow(new IllegalStateException("Unsupported policy unit: WEEKS"));

                    // when
                    byte earnedPoints = exerciseScoreCommandService.awardPointsForExercise(exercise);

                    // then
                    assertThat(earnedPoints).isZero();
                    assertThat(member.getMemberScore()).isEqualTo((byte) 50); // 점수 변동 없음
                } finally{
                    RequestContextHolder.resetRequestAttributes();
                }
            }

            @Test
            @DisplayName("관리자에게 슬랙으로 에러 알림을 보낸다")
            void sends_error_to_admin_using_slack(){
                UUID memberId = UUID.randomUUID();
                String requestUri = "/api/v1/exercise/award";
                String requestMethod = "POST";

                setUpExceptionTestContext(requestUri, requestMethod);

                try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                    // given
                    userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(memberId);


                    Member member = createMemberWithScore((byte) 50);
                    Exercise exercise = createExerciseFor(member, LocalDateTime.now());
                    given(exercisePolicyValidator.calculateValidityEndDate()).willThrow(new IllegalStateException("DB Connection Failed"));

                    // when
                    exerciseScoreCommandService.awardPointsForExercise(exercise);

                    // then
                    ArgumentCaptor<MemberScoreErrorDto> dtoCaptor = ArgumentCaptor.forClass(MemberScoreErrorDto.class);

                    verify(notifyErrorToAdmin).sendMemberScoreIncreaseErrorToSlack(dtoCaptor.capture());

                    MemberScoreErrorDto capturedDto = dtoCaptor.getValue();

                    Assertions.assertThat(capturedDto.getUserIdentifier()).isEqualTo(memberId);
                    Assertions.assertThat(capturedDto.getHttpMethod()).isEqualTo(requestMethod);
                    Assertions.assertThat(capturedDto.getRequestUri()).isEqualTo(requestUri);
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            }

            private void setUpExceptionTestContext(String requestUri, String requestMethod){
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setRequestURI(requestUri);
                request.setMethod(requestMethod);
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            }
        }
    }
}