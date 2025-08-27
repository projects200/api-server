package com.project200.undabang.common.message.impl;

import com.project200.undabang.common.message.MessageSender;
import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * {@link MessageSender} 인터페이스의 Slack 구현체입니다.
 * Slack Incoming Webhook을 사용하여 지정된 채널로 메시지를 비동기적으로 전송합니다.
 *
 * <h3>주요 기능</h3>
 * <ul>
 *     <li>애플리케이션 속성({@code application.yml})에서 웹훅 URL과 활성화 여부를 주입받습니다.</li>
 *     <li>{@code slack.webhook.enabled} 속성을 통해 실제 메시지 전송 여부를 제어할 수 있습니다.</li>
 *     <li>{@link Async} 어노테이션을 통해 별도의 스레드 풀({@code slackMessageSenderExecutor})에서 메시지를 비동기적으로 전송하여,
 *     호출 스레드의 블로킹을 최소화합니다.</li>
 * </ul>
 */
@Slf4j
@Component
public class SlackMessageSender implements MessageSender {
    private static final Slack slack = Slack.getInstance();

    private final String webhookUrl;
    private final boolean webhookEnabled;

    public SlackMessageSender(@Value("${slack.webhook.url}") String webhookUrl,
                              @Value("${slack.webhook.enabled}") boolean webhookEnabled) {
        this.webhookUrl = webhookUrl;
        this.webhookEnabled = webhookEnabled;
    }

    @Async("slackMessageSenderExecutor")
    @Override
    public void send(String message) {
        try {
            if(!webhookEnabled){ // 전송 제어 기능 추가
                log.info("슬랙 알림 기능 비활성화 상태입니다!");
                return;
            }

            Payload payload = Payload.builder()
                    .text(message)
                    .build();

            WebhookResponse response = slack.send(webhookUrl, payload);

            if(response.getCode() != 200){
                log.warn("Slack 알림 전송 실패. \n 응답코드 : {}, 응답 본문 : {}", response.getCode(), response.getBody());
            }
        } catch (IOException e) {
            log.error("Slack 알림 전송 중 I/O 에러가 발생했습니다", e);
        } catch (Exception e) {
            log.error("Slack 알림 전송 중 예기치 않은 에러가 발생했습니다", e);
        }

    }
}
