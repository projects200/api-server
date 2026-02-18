package com.project200.undabang.common.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public abstract class IntegrationTestSupport {

    // 1. AWS용 LocalStack
    protected static final LocalStackContainer LOCAL_STACK;

    // 2. [추가] Slack API용 WireMock
    protected static final GenericContainer<?> WIREMOCK_CONTAINER;

    static {
        // --- LocalStack 초기화 ---
        LOCAL_STACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                .withServices(LocalStackContainer.Service.S3)
                .withEnv("DEFAULT_REGION", "ap-northeast-2");
        LOCAL_STACK.start();

        // --- [추가] WireMock 초기화 ---
        WIREMOCK_CONTAINER = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.5.2-1"))
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200))
                .withCommand("--verbose");

        WIREMOCK_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // LocalStack 설정
        registry.add("spring.cloud.aws.s3.endpoint", () -> LOCAL_STACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("spring.cloud.aws.credentials.access-key", LOCAL_STACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCAL_STACK::getSecretKey);
        registry.add("spring.cloud.aws.region.static", LOCAL_STACK::getRegion);
    }
}