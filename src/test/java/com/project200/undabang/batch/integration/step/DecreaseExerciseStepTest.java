package com.project200.undabang.batch.integration.step;

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
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@SpringBatchTest
@SpringBootTest
class DecreaseExerciseStepTest {
    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    @Qualifier("decreaseExerciseScoreJob") // 추후 다른 배치가 생길 수 있으니 운동점수 감소 Job 빈만 주입
    Job decreaseExerciseScoreJob;

    @Autowired
    private PolicyService policyService;
    private int policyDueDate;
    private int policyDecreasePoint;

    @BeforeEach
    void setUp(){
        policyDueDate = policyService.getPolicyAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);
        policyDecreasePoint = policyService.getPolicyAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);
    }

    @AfterEach
    void tearDown() {
        exerciseRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("decreaseExerciseScoreStep은 정책에 따라 운동기록이 생성되지 않은 회원의 점수를 감소시킨다")
    void decreaseExerciseScoreStep_executeSucceed() {
        // given
        LocalDateTime runDate = LocalDateTime.of(2020, 1, 15, 0, 0, 0);

        Member activeMember = createMember("activeMember", (byte) 97);
        Exercise activeMemberExercise = createExercise(activeMember, runDate.minusDays(policyDueDate-1), runDate.minusDays(policyDueDate-2));

        Member inActiveMember = createMember("inActiveMember", (byte) 25);
        Exercise inActiveMemberExercise = createExercise(inActiveMember, runDate.minusDays(policyDueDate+1), runDate.minusDays(policyDueDate));

        Member zeroScoreMember = createMember("zeroScoreMember", (byte) 0);
        Exercise zeroScoreMemberExercise = createExercise(zeroScoreMember, runDate.minusYears(1), runDate.minusYears(1).plusDays(1));

        jobLauncherTestUtils.setJob(decreaseExerciseScoreJob);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("runDate", runDate.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("decreaseExerciseScoreStep", jobParameters);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        // then
        Assertions.assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Assertions.assertThat(stepExecution.getReadCount()).isEqualTo(2); // inactive, zeroscore
        Assertions.assertThat(stepExecution.getWriteCount()).isEqualTo(1); // inactive
        Assertions.assertThat(stepExecution.getFilterCount()).isEqualTo(1); // zeroScore

        Member foundInactiveMember = memberRepository.findById(inActiveMember.getMemberId()).orElseThrow();
        Assertions.assertThat(foundInactiveMember.getMemberScore()).isEqualTo((byte) (25) - policyDecreasePoint); // 25-1

        Member foundActiveMember = memberRepository.findById(activeMember.getMemberId()).orElseThrow();
        Assertions.assertThat(foundActiveMember.getMemberScore()).isEqualTo((byte) 97);

        Member foundZeroScoreMember = memberRepository.findById(zeroScoreMember.getMemberId()).orElseThrow();
        Assertions.assertThat(foundZeroScoreMember.getMemberScore()).isEqualTo((byte) 0);
    }

    private Member createMember(String nickname, Byte score){
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
                .memberScore(score)
                .build();

        return memberRepository.save(member);
    }

    private Exercise createExercise(Member member, LocalDateTime startedAt, LocalDateTime endedAt){
        Exercise exercise = Exercise.builder()
                .member(member)
                .exerciseStartedAt(startedAt)
                .exerciseEndedAt(endedAt)
                .exerciseTitle("테스트 운동")
                .build();

        return exerciseRepository.save(exercise);
    }
}