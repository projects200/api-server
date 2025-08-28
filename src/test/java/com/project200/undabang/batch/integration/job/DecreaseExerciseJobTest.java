package com.project200.undabang.batch.integration.job;

import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBatchTest
@SpringBootTest
public class DecreaseExerciseJobTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier("decreaseExerciseScoreJob")
    private Job decreaseExerciseJob;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @MockitoBean
    private PolicyService policyService;

    private final LocalDate RUN_DATE = LocalDate.of(2025, 1, 15);

    @BeforeEach
    void setUp() {
        this.jobLauncherTestUtils.setJob(decreaseExerciseJob);
    }

    @AfterEach
    void tearDown() {
        jobRepositoryTestUtils.removeJobExecutions();
        exerciseRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("운동점수감소 Job 성공한 경우 : 점수가 0보다 크면 1씩 감소, 0인 회원은 유지")
    void decreaseExerciseJob_Success() throws Exception {
        // given
        int policyDueDate = 7;
        int policyDecreasePoint = 1;

        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(policyDecreasePoint);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(policyDueDate);

        Member activeMember = createMember("activeMember", (byte) 77);
        createExercise(activeMember, RUN_DATE.atStartOfDay().minusDays(policyDueDate-1));

        Member inActiveMember = createMember("inActiveMember", (byte) 25);
        createExercise(inActiveMember, RUN_DATE.atStartOfDay().minusDays(policyDueDate+1));

        Member zeroScoreMember = createMember("zeroScoreMember", (byte) 0);
        createExercise(zeroScoreMember, RUN_DATE.atStartOfDay().minusYears(1));

        // when
        // [수정됨] 고유한 파라미터를 생성하는 메서드 호출
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(getUniqueJobParameters(RUN_DATE));

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(2);
        assertThat(stepExecution.getWriteCount()).isEqualTo(1);
        assertThat(stepExecution.getFilterCount()).isEqualTo(1);

        Member decreasedMember = memberRepository.findById(inActiveMember.getMemberId()).orElseThrow();
        Member exercisedMember = memberRepository.findById(activeMember.getMemberId()).orElseThrow();
        Member notActivedMember = memberRepository.findById(zeroScoreMember.getMemberId()).orElseThrow();

        assertThat(decreasedMember.getMemberScore()).isEqualTo((byte) (25 - policyDecreasePoint));
        assertThat(exercisedMember.getMemberScore()).isEqualTo(activeMember.getMemberScore());
        assertThat(notActivedMember.getMemberScore()).isEqualTo(zeroScoreMember.getMemberScore());
    }

    @Test
    @DisplayName("Job 실행중 정책이 변경되어도 현재 Job 실행중 영향이 가면 안된다.")
    void decreaseExerciseJob_initializedOnce_atTheStartOfStep() throws Exception {
        // given
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(1);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(7);

        Member targetMember = createMember("targetMember", (byte) 77);
        createExercise(targetMember, RUN_DATE.atStartOfDay().minusDays(8));

        // when
        // [수정됨] 고유한 파라미터를 생성하는 메서드 호출
        jobLauncherTestUtils.launchJob(getUniqueJobParameters(RUN_DATE));

        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(30);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(3);

        // then
        assertThat(memberRepository.findById(targetMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 76);

        verify(policyService, times(1)).getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);
        verify(policyService, times(1)).getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);
    }

    @Test
    @DisplayName("Job 실행중 정책이 변경되면, 현재 실행중인 Job이 아니라, 다음 Job에 새로운 정책이 적용되야 한다")
    void decreaseExerciseJob_policyChanged_reflectNextJob() throws Exception {
        // given
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(1);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(7);

        Member firstMember = createMember("firstMember", (byte) 77);
        createExercise(firstMember, RUN_DATE.atStartOfDay().minusDays(8));

        Member secondMember = createMember("secondMember", (byte) 45);
        createExercise(secondMember, RUN_DATE.atStartOfDay().minusDays(6));

        // when
        // [수정됨] 첫번째 Job 실행 (고유 파라미터 사용)
        jobLauncherTestUtils.launchJob(getUniqueJobParameters(RUN_DATE));

        // then
        assertThat(memberRepository.findById(firstMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 76);
        assertThat(memberRepository.findById(secondMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 45);

        // when
        // 정책 변경 이후 새로운 Job 실행
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(30);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(3);

        jobLauncherTestUtils.launchJob(getUniqueJobParameters(RUN_DATE));

        //then
        assertThat(memberRepository.findById(firstMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 46);
        assertThat(memberRepository.findById(secondMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 15);

        verify(policyService, times(2)).getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);
        verify(policyService, times(2)).getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);
    }

    private JobParameters getUniqueJobParameters(LocalDate runDate) {
        String uniqueRunDate = runDate.atStartOfDay()
                // LocalTime.now() 대신 고정된 시간 + 나노초를 사용하여 테스트의 재현성을 높입니다.
                .plusNanos(System.nanoTime())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));

        return new JobParametersBuilder()
                .addString("runDate", uniqueRunDate)
                .toJobParameters();
    }

    private Member createMember(String nickname, Byte score){
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
                .memberScore(score)
                .memberCreatedAt(LocalDateTime.now().minusYears(1))
                .build();

        return memberRepository.save(member);
    }

    private Exercise createExercise(Member member, LocalDateTime startedAt){
        Exercise exercise = Exercise.builder()
                .member(member)
                .exerciseStartedAt(startedAt)
                .exerciseEndedAt(startedAt.plusHours(1))
                .exerciseTitle("테스트 운동")
                .build();

        return exerciseRepository.save(exercise);
    }
}
