package com.project200.undabang.common.notification.command.impl;

import com.project200.undabang.common.notification.notifier.Notifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCommandImplTest {

    @Mock
    private Notifier notifier;

    @InjectMocks
    private NotificationCommandImpl notificationCommand;

    @Test
    @DisplayName("sendErrorNotification 호출시 Notifier의 notify() 메소드를 호출한다")
    void sendErrorNotification() {
        // given
        String message = "테스트 메시지";

        // when
        notificationCommand.sendErrorNotification(message);

        // then
        Mockito.verify(notifier, Mockito.times(1)).notify(message);
    }
}