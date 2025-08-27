package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.notification.fcm.dto.NotificationContent;
import com.project200.undabang.notification.fcm.entity.NotificationMessage;
import com.project200.undabang.notification.fcm.entity.NotificationScenario;
import com.project200.undabang.notification.fcm.entity.ScenarioCode;
import com.project200.undabang.notification.fcm.entity.ScenarioMessageMapping;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
@DisplayName("NotificationMessageRepositoryImpl 클래스")
class NotificationMessageRepositoryImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private NotificationMessageRepositoryImpl notificationMessageRepository;

    @Nested
    @DisplayName("findRandomMessageByScenario 메소드는")
    class Context_findRandomMessageByScenario {

        @Test
        @DisplayName("여러 메시지가 매핑된 시나리오 코드 요청 시, 그중 하나를 무작위로 성공적으로 조회한다")
        void givenScenarioWithMultipleMessages_whenFindingRandomMessage_thenReturnsOneContent() {
            // given
            NotificationScenario scenario = NotificationScenario.builder()
                    .scenarioCode(ScenarioCode.POST_INACTIVITY_NUDGE)
                    .scenarioDescription("점수 차감 시작 후 사용자에게 보내는 복귀 유도 알림")
                    .build();
            em.persist(scenario);

            NotificationMessage message2 = NotificationMessage.builder().messageBody("혹시… 저희 앱 삭제하신 줄 알았어요! 돌아오셔서 반가워요. 운동하러 가볼까요?").build();
            NotificationMessage message3 = NotificationMessage.builder().messageBody("주문하신 커피가 식고 있어요… 운다방에 돌아와 주세요 🥺").build();
            NotificationMessage message4 = NotificationMessage.builder().messageBody("점수 회복 챌린지 시작! 지난주보다 더 나은 점수를 위해, 오늘부터 다시 꾸준히 운동해 볼까요? 💪").build();
            em.persist(message2);
            em.persist(message3);
            em.persist(message4);

            em.persist(ScenarioMessageMapping.builder().scenario(scenario).message(message2).build());
            em.persist(ScenarioMessageMapping.builder().scenario(scenario).message(message3).build());
            em.persist(ScenarioMessageMapping.builder().scenario(scenario).message(message4).build());

            em.flush();
            em.clear();

            ScenarioCode scenarioCode = ScenarioCode.POST_INACTIVITY_NUDGE;
            List<String> expectedBodies = List.of(
                    "혹시… 저희 앱 삭제하신 줄 알았어요! 돌아오셔서 반가워요. 운동하러 가볼까요?",
                    "주문하신 커피가 식고 있어요… 운다방에 돌아와 주세요 🥺",
                    "점수 회복 챌린지 시작! 지난주보다 더 나은 점수를 위해, 오늘부터 다시 꾸준히 운동해 볼까요? 💪"
            );

            // when: 랜덤 로직을 검증하기 위해 여러 번 호출하여 다른 결과가 나오는지 확인합니다.
            int maxAttempts = 100; // 최대 시도 횟수
            boolean foundDifferentResult = false;
            int finalAttemptCount = 0; // 실제 시도 횟수를 추적할 변수

            do {
                ++finalAttemptCount; // 시도 횟수 증가
                NotificationContent result = notificationMessageRepository.findRandomMessageByScenario(scenarioCode);

                // then: 매번 조회된 결과가 기본 조건을 만족하는지 우선 확인합니다.
                assertThat(result).as("조회된 알림 내용(시도 " + finalAttemptCount + ")이 null이 아니어야 합니다.").isNotNull();
                assertThat(result.title()).as("메시지 제목(시도 " + finalAttemptCount + ")은 null이어야 합니다.").isNull();
                assertThat(result.body()).as("예상되는 본문(시도 " + finalAttemptCount + ") 중 하나여야 합니다.").isIn(expectedBodies);
                assertThat(result.imageUrl()).as("이미지 URL(시도 " + finalAttemptCount + ")은 null이어야 합니다.").isNull();

                NotificationContent nextResult = notificationMessageRepository.findRandomMessageByScenario(scenarioCode);

                // 첫 번째 결과와 다른 결과가 나왔는지 확인합니다.
                if (!result.body().equals(nextResult.body())) {
                    foundDifferentResult = true;
                    System.out.println("랜덤성 확인: " + finalAttemptCount + "번의 시도 끝에 다른 메시지를 발견했습니다.");
                    break; // 다른 결과가 나왔으므로 반복을 중단합니다.
                }
            } while (finalAttemptCount < maxAttempts);

            // then: 최종적으로 랜덤성이 적용되었는지 검증합니다.
            // 최대 시도 횟수 내에 다른 결과가 한 번이라도 나왔다면 랜덤 로직이 동작한다고 간주합니다.
            assertThat(foundDifferentResult)
                    .as(maxAttempts + "번의 시도 동안 다른 메시지가 한 번 이상 조회되어야 합니다 (랜덤성 검증 실패).")
                    .isTrue();
        }

        @Test
        @DisplayName("단일 메시지가 매핑된 시나리오 코드 요청 시, 해당 메시지를 성공적으로 조회한다")
        void givenScenarioWithSingleMessage_whenFindingRandomMessage_thenReturnsCorrectContent() {
            // given
            NotificationScenario scenario = NotificationScenario.builder()
                    .scenarioCode(ScenarioCode.PRE_INACTIVITY_REMINDER)
                    .scenarioDescription("점수 차감 전 사용자에게 보내는 리마인드 알림")
                    .build();
            em.persist(scenario);

            NotificationMessage message1 = NotificationMessage.builder()
                    .messageBody("잠깐! 소중한 운동 점수가 변동될 수 있어요. 가볍게라도 운동하고 지금의 점수를 지켜볼까요?")
                    .build();
            em.persist(message1);

            em.persist(ScenarioMessageMapping.builder().scenario(scenario).message(message1).build());

            em.flush();
            em.clear();

            ScenarioCode scenarioCode = ScenarioCode.PRE_INACTIVITY_REMINDER;

            // when
            NotificationContent result = notificationMessageRepository.findRandomMessageByScenario(scenarioCode);

            // then
            assertThat(result).as("조회된 알림 내용이 null이 아니어야 합니다.").isNotNull();
            assertThat(result.title()).as("메시지 제목은 null이어야 합니다.").isNull();
            assertThat(result.body()).isEqualTo("잠깐! 소중한 운동 점수가 변동될 수 있어요. 가볍게라도 운동하고 지금의 점수를 지켜볼까요?");
            assertThat(result.imageUrl()).as("이미지 URL은 null이어야 합니다.").isNull();
        }

        @Test
        @DisplayName("주어진 시나리오 코드에 해당하는 메시지가 없을 경우 null을 반환한다")
        void givenScenarioWithoutMessages_whenFindingRandomMessage_thenReturnsNull() {
            // given
            // 테스트 데이터에 없는 시나리오 코드
            ScenarioCode scenarioCodeWithNoMessage = ScenarioCode.POST_INACTIVITY_NUDGE;

            // when
            NotificationContent result = notificationMessageRepository.findRandomMessageByScenario(scenarioCodeWithNoMessage);

            // then
            assertThat(result).as("매핑된 메시지가 없을 경우 결과는 null이어야 합니다.").isNull();
        }
    }
}
