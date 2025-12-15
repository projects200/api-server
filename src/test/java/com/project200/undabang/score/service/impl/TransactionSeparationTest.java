package com.project200.undabang.score.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = { // 빌드시 테스트코드 환경 충돌 문제로 해당 코드 추가
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:test-db-for-tx-test",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@DisplayName("트랜잭션 분리 테스트")
class TransactionSeparationTest {

    @Autowired
    private ExerciseCommandService exerciseCommandService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private EntityManager em;

    // PolicyService만 가짜(Mock)로 대체하여 예외 발생 상황을 제어
    @MockitoBean
    private PolicyService policyService;

    // --- 테스트 시나리오 ---

    @Nested
    @DisplayName("자식 트랜잭션(점수 부여)에서 예외가 발생했을 때")
    class Context_when_child_transaction_fails {

        @Test
        @DisplayName("부모 트랜잭션(운동 기록 생성)은 롤백되지 않고 정상적으로 커밋된다")
        void parent_transaction_should_commit_successfully() {
            // given
            // 1. 자식 트랜잭션(awardPointsForExercise) 내부에서 예외가 발생하도록 설정
            //    checkEarnablePoints 메서드가 PolicyService를 호출할 때 예외를 던지게 만듦
            given(policyService.getPolicyValueAsInt(any()))
                    .willThrow(new RuntimeException("의도적인 정책 조회 실패 예외"));

            // 2. 테스트에 사용할 Member를 생성하고 초기 상태를 DB에 저장
            byte initialScore = 35;
            Member member = Member.builder()
                    .memberId(UUID.randomUUID())
                    .memberNickname("test-user")
                    .memberEmail("test@email.com")
                    .memberScore(initialScore)
                    .build();
            memberRepository.saveAndFlush(member);

            // 3. 운동 기록 생성을 위한 DTO 준비
            // findMember() 로직이 UserContextHolder를 사용한다면, 테스트 전에 컨텍스트 설정이 필요할 수 있습니다.
            CreateExerciseRequestDto requestDto = CreateExerciseRequestDto.builder()
                    .exerciseTitle("운동 기록 생성 테스트")
                    .exercisePersonalType("운동 종류")
                    .exerciseLocation("운동 장소")
                    .exerciseDetail("운동 상세 내용")
                    .exerciseStartedAt(LocalDateTime.now().minusHours(1))
                    .exerciseEndedAt(LocalDateTime.now())
                    .build();

            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(member.getMemberId());

                // when
                // 부모 트랜잭션인 createExercise를 호출합니다.
                // 내부에서 호출된 awardPointsForExercise의 예외는 try-catch로 처리되므로,
                // 이 메서드 호출 자체는 예외를 던지지 않고 정상적으로 완료되어야 합니다.
                exerciseCommandService.createExercise(requestDto);
            }

            // then
            // 1. 부모 트랜잭션의 작업(운동 기록 생성)이 커밋되었는지 검증
            assertThat(exerciseRepository.count()).as("운동 기록은 생성되어야 한다").isEqualTo(1);

            // 2. 자식 트랜잭션의 작업(점수 부여)이 롤백되었는지 검증
            em.clear(); // 1차 캐시를 비워 DB에서 최신 상태를 조회하도록 강제
            Member finalMemberState = memberRepository.findById(member.getMemberId()).orElseThrow();

            assertThat(finalMemberState.getMemberScore())
                    .as("회원 점수는 변하지 않아야 한다")
                    .isEqualTo(initialScore);
        }
    }
}