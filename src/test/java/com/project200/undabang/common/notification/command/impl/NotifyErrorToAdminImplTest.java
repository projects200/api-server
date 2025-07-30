package com.project200.undabang.common.notification.command.impl;

import com.project200.undabang.admin.component.impl.NotifyErrorToAdminImpl;
import com.project200.undabang.common.message.MessageSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifyErrorToAdminImplTest {

    @Mock
    private MessageSender messageSender;

    @InjectMocks
    private NotifyErrorToAdminImpl notificationCommand;

    @Test
    @DisplayName("sendErrorNotification 호출시 Notifier의 notify() 메소드를 호출한다")
    void sendErrorNotification() {
        // given
        String message = "테스트 메시지";

        // when
        notificationCommand.sendErrorNotification(message);

        // then
        Mockito.verify(messageSender, Mockito.times(1)).send(message);
    }
}