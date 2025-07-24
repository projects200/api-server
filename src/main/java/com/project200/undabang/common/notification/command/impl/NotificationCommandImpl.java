package com.project200.undabang.common.notification.command.impl;

import com.project200.undabang.common.notification.command.NotificationCommand;
import com.project200.undabang.common.notification.notifier.Notifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * NotificationCommand 인터페이스의 구현체입니다.
 * 이 클래스는 시스템의 모든 알림 요청을 중앙에서 처리하는 '컨트롤 타워' 역할을 합니다.
 *
 * [설계 결정]
 * 1. 필요한 모든 Notifier(슬랙, RabbitMQ 등) 구현체를 주입받습니다.
 * 2. 각 command 메소드는 비즈니스 규칙에 따라 적절한 Notifier를 선택하여 호출합니다.
 * 3. 새로운 알림 채널(예: RabbitMQ)이 추가되면, 이 클래스의 생성자에 새로운 Notifier를 주입받고,
 *    필요한 command 메소드 내부 로직을 수정/추가하면 됩니다.
 */
@Slf4j
@Component
public class NotificationCommandImpl implements NotificationCommand {
    private final Notifier slackNotifier;

    public NotificationCommandImpl(@Qualifier("slackNotifier") Notifier slackNotifier){
        this.slackNotifier = slackNotifier;
    }

    @Override
    public void sendErrorNotification(String message) {
        log.info("SLACK 채널에 에러 알림을 전송합니다.");
        slackNotifier.notify(message);
    }
}
