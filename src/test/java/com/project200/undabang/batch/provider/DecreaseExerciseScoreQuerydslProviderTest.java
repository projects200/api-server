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
    private final LocalDateTime runDate = LocalDateTime.of(2025, 1, 15, 0, 0, 0);
    private LocalDateTime referenceDate;
    private DecreaseExerciseScoreQuerydslProvider provider;

    @BeforeEach
    void setUp() throws Exception{
        // Mockito를 사용하여 policyService의 메소드가 특정 값을 반환하도록 설정
        Mockito.when(policyService.getPolicyAsInt(PENALTY_INACTIVITY_THRESHOLD_DAYS)).thenReturn(7);

        // 설정된 Mock 객체를 사용하여 필드 초기화
        policyDueDate = policyService.getPolicyAsInt(PENALTY_INACTIVITY_THRESHOLD_DAYS);
        referenceDate = runDate.minusDays(policyDueDate);

        provider = new DecreaseExerciseScoreQuerydslProvider(referenceDate);
        provider.setEntityManager(entityManager);
        provider.afterPropertiesSet();
    }

    @Test
    @DisplayName("성공케이스 _ 마지막 운동일이 기준일이거나, 오래된 회원을 조회하는 경우")
    void queryProvider_findOnlyTargetMember() throws Exception {
        // given
        Member testMember1 = createMember("testMember1"); // 배치 조회에 걸림 (기준일 보다 하루 초과)
        Exercise testMember1Exercise = createExercise(testMember1, referenceDate.minusDays(1), referenceDate.minusDays(1));

        Member testMember2 = createMember("testMember2"); // 배치 조회에 안걸림 (기준일 하루 이전)
        Exercise testMember2Exercise = createExercise(testMember2, referenceDate.plusDays(1), referenceDate.plusDays(1));

        Member testMember3 = createMember("testMember3"); // 배치 조회에 안걸림 (기준일)
        Exercise testMember3Exercise = createExercise(testMember3, referenceDate, referenceDate);

        //when
        Query query = provider.createQuery();

        List<Member> resultMemberList = query.getResultList();

        Assertions.assertThat(resultMemberList).hasSize(1);
        Assertions.assertThat(resultMemberList.get(0).getMemberId()).isEqualTo(testMember1.getMemberId());
    }

    @Test
    @DisplayName("성공케이스 _ 운동 기록이 전혀 없는 회원을 조회하는 경우")
    void queryProvider_findNoExerciseMember() throws Exception {
        // given (가입일이 오래되고 운동 기록이 없는 회원)
        Member memberWithoutExercise = Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname("memberWithoutExercise")
                .memberEmail("e@Mail.com")
                .memberCreatedAt(referenceDate.minusDays(1))
                .build();

        Member newMember = Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname("newMember")
                .memberEmail("e2@mail.com")
                .memberCreatedAt(referenceDate.plusDays(1))
                .build();

        memberRepository.save(memberWithoutExercise);
        memberRepository.save(newMember);

        // when
        Query query = provider.createQuery();
        List<Member> resultMemberList = query.getResultList();

        // then
        Assertions.assertThat(resultMemberList).hasSize(1);
        Assertions.assertThat(resultMemberList.get(0).getMemberId()).isEqualTo(memberWithoutExercise.getMemberId());
    }

    @Test
    @DisplayName("성공케이스 _ 기준일에 해당하는 회원이 조회되는지 확인하는 경우")
    void queryProvider_findPolicyDateMember(){
        // given
        Member findMember = createMember("findMember");
        Exercise findMemberExercise = createExercise(findMember, referenceDate, referenceDate);

        Member notFindMember = createMember("notFindMember");
        Exercise notFindMemberExercise = createExercise(notFindMember, referenceDate.plusDays(1), referenceDate.plusDays(2));

        // when
        Query query = provider.createQuery();
        List<Member> resultMemberList = query.getResultList();

        // then
        Assertions.assertThat(resultMemberList).hasSize(0);
    }

    @Test
    @DisplayName("성공케이스 _ 조회할 회원이 없는 경우")
    void queryProvider_findMembers(){
        // given
        Member activeMember1 = createMember("activeUser1");
        createExercise(activeMember1, runDate.minusDays(1), runDate.minusDays(1));
        Member activeMember2 = createMember("activeUser2");
        createExercise(activeMember2, runDate.minusDays(5), runDate.minusDays(5));

        // when
        Query query = provider.createQuery();
        List<Member> resultMemberList = query.getResultList();

        // then
        Assertions.assertThat(resultMemberList).isEmpty();
    }

    @Test
    @DisplayName("실패: 탈퇴한 회원은 조건에 맞아도 조회하지 않는다")
    void queryProvider_doesNotFind_deletedMember() {
        // given
        Member deletedMember = Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname("deletedMember")
                .memberEmail("d@eleted.com")
                .memberCreatedAt(referenceDate.minusYears(2))
                .memberDeletedAt(referenceDate.minusDays(1))
                .build();

        memberRepository.save(deletedMember);
        createExercise(deletedMember, referenceDate.minusDays(10), referenceDate.minusDays(10));

        // when
        Query query = provider.createQuery();
        List<Member> resultMemberList = query.getResultList();

        // then
        Assertions.assertThat(resultMemberList).isEmpty();
    }

    @Test
    @DisplayName("성공: 삭제된 운동 기록은 없는 것으로 간주하고 회원을 조회한다")
    void queryProvider_ignoresDeletedExercise_andFindsMember() {
        // given
        Member member = createMember("userWithDeletedExercise");

        Exercise oldExercise = createExercise(member, referenceDate.minusDays(1), referenceDate.minusDays(1));

        Exercise deletedExercise = Exercise.builder()
                .member(member)
                .exerciseTitle("userWithDeletedExercise")
                .exerciseStartedAt(referenceDate.minusDays(1))
                .exerciseEndedAt(referenceDate.minusDays(1))
                .exerciseCreatedAt(runDate)
                .exerciseDeletedAt(referenceDate.minusDays(1))
                .build();

        exerciseRepository.save(deletedExercise);

        // when
        Query query = provider.createQuery();
        List<Member> resultMemberList = query.getResultList();

        // then
        Assertions.assertThat(resultMemberList).hasSize(1);
        Assertions.assertThat(resultMemberList.get(0).getMemberId()).isEqualTo(member.getMemberId());
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