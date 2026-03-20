package com.project200.undabang.common.message.impl;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.project200.undabang.common.message.MessageSender;
import com.project200.undabang.common.support.IntegrationTestSupport;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.score.validation.ExercisePolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;

//@Testcontainers
@SpringBootTest
class SlackMessageSenderTest extends IntegrationTestSupport {

//    @Container
//    static GenericContainer<?> wiremockContainer = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.5.2-1"))
//            .withExposedPorts(8080)
//            .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200))
//            .withCommand("--verbose")
//            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(SlackMessageSenderTest.class)));

    @Nested
    @SpringBootTest
    @DisplayName("Slack 활성화 상태 테스트")
    class SlackEnabledIntegrationTest {

        @Autowired
        private MessageSender messageSender;

        @MockitoBean
        private PolicyService policyService;
        @MockitoBean
        private ExerciseRepository exerciseRepository;
        @MockitoBean
        private ExercisePolicyValidator exercisePolicyValidator;

        @DynamicPropertySource
        static void overrideProperties(DynamicPropertyRegistry registry) {
            String webhookPath = "/mock/slack-webhook";
            registry.add("slack.webhook.url",
                    () -> String.format("http://%s:%d%s",
                            WIREMOCK_CONTAINER.getHost(),
                            WIREMOCK_CONTAINER.getFirstMappedPort(),
                            webhookPath));
//            registry.add("slack.webhook.url",
//                    () -> String.format("http://%s:%d%s", wiremockContainer.getHost(), wiremockContainer.getFirstMappedPort(), webhookPath));
            registry.add("slack.webhook.enabled", () -> "true");
        }

        @BeforeEach
        void setUp() {
            WireMock.configureFor(WIREMOCK_CONTAINER.getHost(), WIREMOCK_CONTAINER.getFirstMappedPort());
            WireMock.reset();
        }

        @Test
        @DisplayName("Slack API 가 200을 반환하면 정상처리")
        void slackNotifierAdapterSuccessCondition() {
            // given
            String message = "테스트 메시지 _ 테스트 코드에서 성공적으로 통과함";
            String webhookPath = "/mock/slack-webhook";

            stubFor(post(urlEqualTo(webhookPath))
                    .willReturn(aResponse().withStatus(200).withBody("ok")));

            // when
            messageSender.send(message);

            // then
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(
                    () -> WireMock.verify(1, postRequestedFor(urlEqualTo(webhookPath))
                            .withRequestBody(containing(message)))
            );
        }

        @Test
        @DisplayName("Slack API 가 500 에러를 반환하면 경고 로그를 남김")
        void slackNotifierAdapterFailureCondition() {
            // given
            String message = "테스트 메시지 _ 테스트 코드에서 실패함";
            String webhookPath = "/mock/slack-webhook";

            stubFor(post(urlEqualTo(webhookPath))
                    .willReturn(aResponse().withStatus(500).withBody("error")));

            // when
            messageSender.send(message);

            // then
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(
                    () -> WireMock.verify(1, postRequestedFor(urlEqualTo(webhookPath))
                            .withRequestBody(containing(message)))
            );
        }

        @Test
        @DisplayName("Slack Webhook URL이 유효하지 않아 403 에러를 반환하면 경고 로그를 남김")
        void slackNotifierAdapterInvalidUrlCondition() {
            // given
            String message = "테스트 메시지 _ 잘못된 Webhook URL";
            String webhookPath = "/mock/slack-webhook";

            // mock stub 설정 - 403 Forbidden 응답을 반환하도록 설정
            stubFor(post(urlEqualTo(webhookPath))
                    .willReturn(aResponse()
                            .withStatus(403)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("invalid_auth")));

            // when
            messageSender.send(message);

            // then
            // 비동기 호출이므로 Awaitility를 사용하여 요청이 시도되었는지 검증
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(
                    () -> WireMock.verify(1, postRequestedFor(urlEqualTo(webhookPath))
                            .withRequestBody(containing(message)))
            );
        }

        @Test
        @DisplayName("Slack API 통신 중 IOException이 발생하면 에러 로그를 남긴다")
        void slackNotifierAdapterIOExceptionCondition(){
            // given
            String message = "테스트 메시지 _ IOException 발생";
            String webhookPath = "/mock/slack-webhook";

            // mock stub 설정 - CONNECTION_RESET_BY_PEER Fault를 발생시켜 IOException을 유도
            stubFor(post(urlEqualTo(webhookPath))
                    .willReturn(aResponse()
                            .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

            // when
            messageSender.send(message);

            // then
            // 비동기 호출이므로 Awaitility를 사용하여 요청이 시도되었는지 검증
            // IOE 발생시 내부 재시도를 하므로 2번 호출
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(
                    () -> WireMock.verify(2, postRequestedFor(urlEqualTo(webhookPath))
                            .withRequestBody(containing(message)))
            );
        }
    }

    @Nested
    @SpringBootTest
    @DisplayName("Slack 비활성화 상태 테스트")
    class SlackDisabledIntegrationTest {

        @Autowired
        private MessageSender messageSender;

        @MockitoBean
        private PolicyService policyService;
        @MockitoBean
        private ExerciseRepository exerciseRepository;
        @MockitoBean
        private ExercisePolicyValidator exercisePolicyValidator;

        @DynamicPropertySource
        static void overrideProperties(DynamicPropertyRegistry registry) {
            String webhookPath = "/mock/slack-webhook";
//            registry.add("slack.webhook.url",
//                    () -> String.format("http://%s:%d%s", wiremockContainer.getHost(), wiremockContainer.getFirstMappedPort(), webhookPath));
            registry.add("slack.webhook.url",
                    () -> String.format("http://%s:%d%s",
                            WIREMOCK_CONTAINER.getHost(),
                            WIREMOCK_CONTAINER.getFirstMappedPort(),
                            webhookPath));
            registry.add("slack.webhook.enabled", () -> "false");
        }

        @BeforeEach
        void setUp() {
            WireMock.configureFor(WIREMOCK_CONTAINER.getHost(), WIREMOCK_CONTAINER.getFirstMappedPort());
            WireMock.reset();
        }

        @Test
        @DisplayName("slack.webhook.enabled가 false이면 슬랙 알림을 보내지 않는다")
        void givenWebhookDisabled_whenSend_thenNoRequestIsSent() {
            // given
            String message = "이 메시지는 전송되면 안됩니다.";
            String webhookPath = "/mock/slack-webhook";

            // when
            messageSender.send(message);

            // then
            await().pollDelay(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(
                    () -> WireMock.verify(0, postRequestedFor(urlEqualTo(webhookPath)))
            );
        }
    }
}