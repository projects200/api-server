package com.project200.undabang.common.support;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public abstract class IntegrationTestSupport {

    // 컨테이너 객체들 (실패 시 null이 될 수 있음)
    protected static final LocalStackContainer LOCAL_STACK;
    protected static final GenericContainer<?> WIREMOCK_CONTAINER;

    // 도커 사용 가능 여부 플래그
    protected static final boolean IS_DOCKER_AVAILABLE;

    static {
        LocalStackContainer localStack = null;
        GenericContainer<?> wireMock = null;
        boolean isAvailable = false;

        try {
            // 1. LocalStack 시도
            localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                    .withServices(LocalStackContainer.Service.S3)
                    .withEnv("DEFAULT_REGION", "ap-northeast-2");
            localStack.start();

            // 2. WireMock 시도
            wireMock = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.5.2-1"))
                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200))
                    .withCommand("--verbose");
            wireMock.start();

            // 여기까지 오면 성공
            isAvailable = true;
            System.out.println("Docker 컨테이너 시작 성공");

        } catch (Throwable t) {
            // [핵심] Exception이 아니라 Throwable을 잡아야 'ExceptionInInitializerError'를 막을 수 있음
            System.err.println("Docker 연결 실패! 통합 테스트를 위한 컨테이너를 시작할 수 없습니다.");
            System.err.println("원인: " + t.getMessage());
            // 에러를 던지지 않고 먹어버립니다. (클래스 로딩 성공을 위해)

            // 실패 시 컨테이너 종료 시도 (리소스 정리)
            if (localStack != null) try {
                localStack.stop();
            } catch (Exception e) {
            }
            if (wireMock != null) try {
                wireMock.stop();
            } catch (Exception e) {
            }

            localStack = null;
            wireMock = null;
            isAvailable = false;
        }

        LOCAL_STACK = localStack;
        WIREMOCK_CONTAINER = wireMock;
        IS_DOCKER_AVAILABLE = isAvailable;
    }

    /**
     * 모든 테스트 실행 전에 Docker 가용성을 체크합니다.
     * Docker가 없으면 테스트를 '실패'가 아니라 '스킵(Ignored)' 처리합니다.
     */
    @BeforeAll
    static void checkDockerRequirement() {
        Assumptions.assumeTrue(IS_DOCKER_AVAILABLE,
                "Docker 환경을 찾을 수 없어 통합 테스트를 건너뜁니다 (CI 환경 이슈 등).");
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // 도커가 실행 중일 때만 진짜 설정을 넣습니다.
        if (IS_DOCKER_AVAILABLE && LOCAL_STACK != null && WIREMOCK_CONTAINER != null) {
            // LocalStack
            registry.add("spring.cloud.aws.s3.endpoint", () -> LOCAL_STACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
            registry.add("spring.cloud.aws.credentials.access-key", LOCAL_STACK::getAccessKey);
            registry.add("spring.cloud.aws.credentials.secret-key", LOCAL_STACK::getSecretKey);
            registry.add("spring.cloud.aws.region.static", LOCAL_STACK::getRegion);

            // WireMock
            // 주의: 자식 클래스에서 호출할 때 WIREMOCK_CONTAINER가 null이면 안되므로 여기서 처리하거나,
            // 자식 클래스의 @DynamicPropertySource에서도 IS_DOCKER_AVAILABLE 체크가 필요할 수 있음.
            // 하지만 @BeforeAll에서 assumeTrue로 막았기 때문에 여기까지 진입해도 테스트는 이미 스킵됨.
        } else {
            // [중요] 도커가 죽었을 때 Spring Context가 깨지지 않게 '가짜 값'을 넣어줍니다.
            // 이렇게 해야 'ApplicationContext load failed' 에러를 막을 수 있습니다.
            registry.add("spring.cloud.aws.s3.endpoint", () -> "http://failed-docker");
            registry.add("spring.cloud.aws.credentials.access-key", () -> "dummy");
            registry.add("spring.cloud.aws.credentials.secret-key", () -> "dummy");
            registry.add("spring.cloud.aws.region.static", () -> "ap-northeast-2");

            registry.add("slack.webhook.url", () -> "http://failed-docker");
        }
    }
}