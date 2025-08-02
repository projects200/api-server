package com.project200.undabang.common.message.impl;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.project200.undabang.common.message.MessageSender;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.score.service.ExerciseScoreCommandService;
import com.project200.undabang.score.validation.ExercisePolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
class SlackMessageSenderTest {

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private ExerciseScoreCommandService exerciseScoreCommandService;

    @MockitoBean
    private PolicyService policyService;

    @MockitoBean
    private ExerciseRepository exerciseRepository;

    @MockitoBean
    private ExercisePolicyValidator exercisePolicyValidator;

    // @Container: Testcontainers가 이 컨테이너의 생명주기(시작, 종료)를 관리하게 합니다.
    @Container
    static GenericContainer<?> wiremockContainer = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.5.2-1"))
            .withExposedPorts(8080) // 컨테이너의 8080 포트를 외부에서 사용할 수 있도록 함
            .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200)) // WireMock이 완전히 실행될 때 까지 기다림
            .withCommand("--verbose") // WireMock Container가 --verbose 옵션으로 실행되서 wiremock이 받은 요청과 응답에 대한 상세 로그를 출력한다
            // 테스트 실행 시 콘솔에서 WireMock 로그를 확인할 수 있음
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(SlackMessageSenderTest.class)));


    // @DynamicPropertySource: Spring 애플리케이션의 프로퍼티를 동적으로 설정합니다.
    // 컨테이너가 뜬 후에야 알 수 있는 동적 포트 정보를 애플리케이션에 알려주기 위해 사용됩니다.
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        String webhookPath = "/mock/slack-webhook";
        registry.add("slack.webhook.url",
                () -> String.format("http://%s:%d%s", wiremockContainer.getHost(), wiremockContainer.getFirstMappedPort(), webhookPath));
        registry.add("slack.webhook.enabled", () -> "true");
        // wiremockContainer.getHost() -> "localhost"
        // wiremockContainer.getFirstMappedPort() -> 매번 바뀌는 랜덤 포트
    }

    @Nested
    @DisplayName("send 메서드 테스트 - slackMessageSender와 외부 HTTP 통신 테스트")
    class sendMethod {

        @BeforeEach
        void setUp() {
            // WireMock의 정적 클라이언트(stubFor, verify 등)가 어떤 서버를 대상으로 할지 설정합니다.
            // 이 설정을 통해 Testcontainers가 띄운 동적 주소의 WireMock 서버를 제어할 수 있게 됩니다.
            WireMock.configureFor(wiremockContainer.getHost(), wiremockContainer.getFirstMappedPort());
            WireMock.reset();
        }

        @Test
        @DisplayName("Slack API 가 200을 반환하면 정상처리")
        void slackNotifierAdapterSuccessCondition(){
            // given
            String message = "테스트 메시지 _ 테스트 코드에서 성공적으로 통과함";
            String webhookPath = "/mock/slack-webhook";

            // mock stub 설정
            stubFor(post(urlEqualTo(webhookPath))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("ok")));


            // when
            messageSender.send(message);

            // then
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(
                    () -> verify(1, postRequestedFor(urlEqualTo(webhookPath))
                            .withRequestBody(containing(message)))
            );
        }

        @Test
        @DisplayName("Slack API 가 500 에러를 반환하면 경고 로그를 남김")
        void slackNotifierAdapterFailureCondition(){
            // given
            String message = "테스트 메시지 _ 테스트 코드에서 실패함";
            String webhookPath = "/mock/slack-webhook";

            stubFor(post(urlEqualTo(webhookPath))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("error")));

            // when
            messageSender.send(message);

            // then
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(
                    () -> verify(1, postRequestedFor(urlEqualTo(webhookPath))
                            .withRequestBody(containing(message)))
            );
        }
    }
}