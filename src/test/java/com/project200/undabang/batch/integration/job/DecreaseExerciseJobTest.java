package com.project200.undabang.batch.integration.job;

import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.assertj.core.api.Assertions;
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

    private JobParameters getJobParameters(LocalDate runDate) {
        return new JobParametersBuilder()
                .addString("runDate", runDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }

    @Test
    @DisplayName("운동점수감소 Job 성공한 경우 : 점수가 0보다 크면 1씩 감소, 0인 회원은 유지")
    void decreaseExerciseJob_Success() throws Exception{
        // given
        int policyDueDate = 7;
        int policyDecreasePoint = 1;

        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(policyDecreasePoint);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(policyDueDate);

        // 점수 감소 대상이 아닌 유저
        Member activeMember = createMember("activeMember", (byte) 77);
        createExercise(activeMember, RUN_DATE.atStartOfDay().minusDays(policyDueDate-1));

        // 점수 감소 대상인 유저
        Member inActiveMember = createMember("inActiveMember", (byte) 25);
        createExercise(inActiveMember, RUN_DATE.atStartOfDay().minusDays(policyDueDate+1));

        // 점수 감소 대상이지만, 점수가 없는 유저
        Member zeroScoreMember = createMember("zeroScoreMember", (byte) 0);
        createExercise(zeroScoreMember, RUN_DATE.atStartOfDay().minusYears(1));

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(getJobParameters(RUN_DATE));

        // then
        // job 실행결과 검증
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // step 검증
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        Assertions.assertThat(stepExecution.getReadCount()).isEqualTo(2); // 2명 읽는게 맞음 (zero, inactive)
        Assertions.assertThat(stepExecution.getWriteCount()).isEqualTo(1); // 1명만 Processor에서 필터링 됨 (zeroScore filtering)
        Assertions.assertThat(stepExecution.getFilterCount()).isEqualTo(1); // 1명만 디비에 업데이트 된 값이 저장되는게 맞음 (inactive -> db)

        // 데이터베이스 상태 검증
        Member decreasedMember = memberRepository.findById(inActiveMember.getMemberId()).orElseThrow();
        Member exercisedMember = memberRepository.findById(activeMember.getMemberId()).orElseThrow();
        Member notActivedMember = memberRepository.findById(zeroScoreMember.getMemberId()).orElseThrow();

        Assertions.assertThat(decreasedMember.getMemberScore()).isEqualTo((byte) (25 - policyDecreasePoint));
        Assertions.assertThat(exercisedMember.getMemberScore()).isEqualTo(activeMember.getMemberScore());
        Assertions.assertThat(notActivedMember.getMemberScore()).isEqualTo(zeroScoreMember.getMemberScore());
    }

    @Test
    @DisplayName("Job 실행중 정책이 변경되어도 현재 Job 실행중 영향이 가면 안된다.")
    void decreaseExerciseJob_initializedOnce_atTheStartOfStep() throws Exception{
        // given
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(1);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(7);

        // 조건에 부합하는 회원 (마지막 운동 8일 전)
        Member targetMember = createMember("targetMember", (byte) 77);
        createExercise(targetMember, RUN_DATE.atStartOfDay().minusDays(8));

        // when (Job 전체를 실행한다)
        jobLauncherTestUtils.launchJob(getJobParameters(RUN_DATE));

        // Job이 끝난 후 정책이 변경되었다 가정함
        // 그 중간에 정책이 바뀌어도 현재 작업에 영향을 주어선 안된다.
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(30);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(3);

        // then
        // 중간에 정책이 바뀌어도, 맨 처음 적용한 정책이 반영되어야 함
        Assertions.assertThat(memberRepository.findById(targetMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 76);

        // step이 동작하는 동안 Reader와 Process는 동작하지 않고, Step 시작시 초기화 된 값을 계속 사용해야 한다.
        // StepScope빈 은 Step이 시작될 때, 단 한번만 인스턴스화 되므로 정확히 한번만 호출됨을 확인하면 외부 정책이 변경되어도 현재 Step의 동작은 영향을 받지 않는다는 사실이 보장됨
        verify(policyService, times(1)).getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);
        verify(policyService, times(1)).getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);
    }

    @Test
    @DisplayName("Job 실행중 정책이 변경되면, 현재 실행중인 Job이 아니라, 다음 Job에 새로운 정책이 적용되야 한다")
    void decreaseExerciseJob_policyChanged_reflectNextJob() throws Exception{
        // given
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(1);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(7);

        // 조건에 부합하는 회원 (마지막 운동 8일 전)
        Member firstMember = createMember("firstMember", (byte) 77);
        createExercise(firstMember, RUN_DATE.atStartOfDay().minusDays(8));

        // 조건에 부합하지 않는 회원 (마지막 운동 6일 전)
        Member secondMember = createMember("secondMember", (byte) 45);
        createExercise(secondMember, RUN_DATE.atStartOfDay().minusDays(6));

        // when
        // 첫번째 Job 실행
        jobLauncherTestUtils.launchJob(getJobParameters(RUN_DATE));

        // then
        // firstMember만 감소되야 함
        Assertions.assertThat(memberRepository.findById(firstMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 76);
        Assertions.assertThat(memberRepository.findById(secondMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 45);

        // when
        // 정책 변경 이후 새로운 Job 실행
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).willReturn(30);
        given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(3);

        jobLauncherTestUtils.launchJob(getJobParameters(RUN_DATE));

        //then
        Assertions.assertThat(memberRepository.findById(firstMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 46);
        Assertions.assertThat(memberRepository.findById(secondMember.getMemberId()).get().getMemberScore()).isEqualTo((byte) 15);

        // 정책 테이블 호출이 두번 이루어 졌는지 확인
        verify(policyService, times(2)).getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);
        verify(policyService, times(2)).getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);
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
