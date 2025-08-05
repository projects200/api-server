package com.project200.undabang.admin.component.impl;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.CommonErrorDto;
import com.project200.undabang.common.message.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * NotifyErrorToAdmin 인터페이스의 구현체입니다.
 * 이 클래스는 시스템의 모든 알림 요청을 중앙에서 처리하는 역할을 합니다.
 *
 * [설계 결정]
 * 1. 필요한 모든 Notifier(슬랙, RabbitMQ 등) 구현체를 주입받습니다.
 * 2. 각 command 메소드는 비즈니스 규칙에 따라 적절한 Notifier를 선택하여 호출합니다.
 * 3. 새로운 알림 채널(예: RabbitMQ)이 추가되면, 이 클래스의 생성자에 새로운 Notifier를 주입받고,
 *    필요한 command 메소드 내부 로직을 수정/추가하면 됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyErrorToAdminImpl implements NotifyErrorToAdmin {
    private final MessageSender slackMessageSender;

    // CommonErrorDto 를 상속받는 DTO 들이 Slack API에 에러 알림을 보낼 때 사용하는 메소드입니다.
    @Override
    public void sendErrorToSlackApi(CommonErrorDto dto) {
        slackMessageSender.send(dto.formattingMessage());
    }
}
