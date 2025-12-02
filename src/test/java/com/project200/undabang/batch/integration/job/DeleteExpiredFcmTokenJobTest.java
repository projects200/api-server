package com.project200.undabang.batch.integration.job;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBatchTest
@SpringBootTest
public class DeleteExpiredFcmTokenJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private Job deleteExpiredFcmTokenJob;

    @BeforeEach
    void setUp() {
        this.jobLauncherTestUtils.setJob(deleteExpiredFcmTokenJob);
        cleanUp(); // Clean before each test
    }

    @AfterEach
    void tearDown() {
        cleanUp(); // Clean after each test
    }

    @Test
    @DisplayName("만료된 토큰 삭제 Job 실행 시 만료된 토큰만 삭제되고 유효한 토큰은 남아야 한다")
    void deleteExpiredFcmTokenJob_Success() throws Exception {
        // Given
        Member member = createMember();

        // 1. 만료된 토큰 생성 (어제 기준 만료됨 -> 삭제 대상)
        createToken(member, "expired_token_1", LocalDateTime.now().minusDays(1));
        createToken(member, "expired_token_2", LocalDateTime.now().minusDays(10));

        // 2. 유효한 토큰 생성 (내일 만료됨 -> 유지 대상)
        createToken(member, "valid_token_1", LocalDateTime.now().plusDays(1));

        // 데이터 검증: 총 3개가 잘 들어갔는지
        assertThat(fcmTokenRepository.count()).isEqualTo(3);

        // When
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("datetime", LocalDateTime.now().toString()) // 유니크 파라미터
                .toJobParameters();

        // 배치 Job 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        // 1. Job 성공 여부
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 2. 결과 데이터 검증
        // 3개 중 만료된 2개가 삭제되어 1개만 남아야 함
        long count = fcmTokenRepository.count();
        assertThat(count).isEqualTo(1);

        // 남은 토큰이 'valid_token_1'인지 확인
        FcmToken remainingToken = fcmTokenRepository.findAll().get(0);
        assertThat(remainingToken.getFcmTokenValue()).isEqualTo("valid_token_1");
    }

    private Member createMember() {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("test@test.com")
                .memberNickname("TestNick")
                .memberGender(MemberGender.UNKNOWN)
                .build();

        return memberRepository.save(member);
    }

    private void createToken(Member member, String tokenValue, LocalDateTime expiredAt) {
        FcmToken token = FcmToken.builder()
                .member(member)
                .fcmTokenValue(tokenValue)
                .fcmTokenExpiredAt(expiredAt)
                .fcmTokenIsActive(true)
                .build();

        fcmTokenRepository.save(token);
    }

    private void cleanUp() {
        fcmTokenRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

}
