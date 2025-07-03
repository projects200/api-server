package com.project200.undabang.batch.provider;

import com.project200.undabang.common.batch.provider.DecreaseExerciseScoreQuerydslProvider;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.policy.entity.PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class DecreaseExerciseScoreQuerydslProviderTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @MockitoBean
    private PolicyService policyService;

    private int policyDueDate;

    @BeforeEach
    void setUp(){
        // Mockito를 사용하여 policyService의 메소드가 특정 값을 반환하도록 설정
        Mockito.when(policyService.getPolicyAsInt(PENALTY_INACTIVITY_THRESHOLD_DAYS)).thenReturn(7);

        // 설정된 Mock 객체를 사용하여 필드 초기화
        policyDueDate = policyService.getPolicyAsInt(PENALTY_INACTIVITY_THRESHOLD_DAYS);
    }

    @Test
    @DisplayName("QueryProvider는 정책에 따라 운동기록이 없는 회원만 조회해야 한다.")
    void queryProvider_findOnlyTargetMember() {
        // given
        LocalDateTime runDate = LocalDateTime.of(2020, 1, 15, 0, 0, 0);

        Member testMember1 = createMember("testMember1"); // 배치 조회에 걸림
        Exercise testMember1Exercise = createExercise(testMember1, runDate.minusDays(policyDueDate+1), runDate.minusWeeks(policyDueDate));

        Member testMember2 = createMember("testMember2"); // 배치 조회에 안걸림
        Exercise testMember2Exercise = createExercise(testMember2, runDate.minusMonths(1), runDate.minusMonths(1).plusDays(1));
        Exercise testMember2Exercise2 = createExercise(testMember2, runDate.minusYears(1), runDate.minusYears(1).plusDays(1));
        Exercise testMember2Exercise3 = createExercise(testMember2, runDate.minusDays(policyDueDate-1), runDate.minusDays(policyDueDate-2));


        LocalDateTime referenceDate = runDate.minusDays(policyDueDate); // 정책에 따라 N 주 이상 운동 안한 회원들 조회

        //when
        DecreaseExerciseScoreQuerydslProvider provider = new DecreaseExerciseScoreQuerydslProvider(referenceDate);
        provider.setEntityManager(entityManager);

        Query query = provider.createQuery();

        List<Member> resultMemberList = query.getResultList();

        Assertions.assertThat(resultMemberList).hasSize(1);
        Assertions.assertThat(resultMemberList.get(0).getMemberId()).isEqualTo(testMember1.getMemberId());
    }

    private Member createMember(String nickname){
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
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