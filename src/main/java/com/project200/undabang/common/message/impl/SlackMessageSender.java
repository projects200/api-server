package com.project200.undabang.common.message.impl;

import com.project200.undabang.common.message.MessageSender;
import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component("slackNotifier")
public class SlackMessageSender implements MessageSender {
    private static final Slack slack = Slack.getInstance();
    private final String webhookUrl;


    public SlackMessageSender(@Value("${notification.slack.webhook-url}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void notify(String message) {
        try {
            Payload payload = Payload.builder()
                    .text(message)
                    .build();

            WebhookResponse response = slack.send(webhookUrl, payload);

            if(response.getCode() != 200){
                log.warn("Slack 알림 전송 실패. \n 응답코드 : {}, 응답 본문 : {}", response.getCode(), response.getBody());
            }
        } catch (IOException e) {
            log.error("Slack 알림 전송 중 I/O 에러가 발생했습니다", e);
        }

    }
}
