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
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@SpringBatchTest
@SpringBootTest
public class DecreaseExerciseJobTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job decreaseExerciseJob;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private PolicyService policyService;

    private int policyDueDate;
    private int policyDecreasePoint;

    @BeforeEach
    void setUp() {
        this.jobLauncherTestUtils.setJob(decreaseExerciseJob);

        this.policyDueDate = policyService.getPolicyAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);
        this.policyDecreasePoint = policyService.getPolicyAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);

        tearDown();
    }

    @AfterEach
    void tearDown() {
        exerciseRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("운동점수감소 Job 성공한 경우 : 점수가 0보다 크면 1씩 감소, 0인 회원은 유지")
    void decreaseExerciseJob_Success() throws Exception{
        // given
        LocalDateTime runDate = LocalDateTime.of(2025, 1, 15, 0, 0, 0);

        // 점수 감소 대상이 아닌 유저
        Member activeMember = createMember("activeMember", (byte) 77);
        Exercise activeMemberExercise = createExercise(activeMember, runDate.minusDays(policyDueDate-1));

        // 점수 감소 대상인 유저
        Member inActiveMember = createMember("inActiveMember", (byte) 25);
        Exercise inActiveMemberExercise = createExercise(inActiveMember, runDate.minusDays(policyDueDate+1));

        // 점수 감소 대상이지만, 점수가 없는 유저
        Member zeroScoreMember = createMember("zeroScoreMember", (byte) 0);
        Exercise zeroScoreMemberExercise = createExercise(zeroScoreMember, runDate.minusYears(1));

        // JobParameter 설정
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("runDate", runDate.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

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
