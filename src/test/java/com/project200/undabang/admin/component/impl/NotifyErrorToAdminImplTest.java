package com.project200.undabang.admin.component.impl;

import com.project200.undabang.admin.entity.dto.BatchErrorDto;
import com.project200.undabang.admin.entity.dto.MemberScoreErrorDto;
import com.project200.undabang.common.message.MessageSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotifyErrorToAdminImplTest {

    @InjectMocks
    private NotifyErrorToAdminImpl notifyErrorToAdmin;

    @Mock
    private MessageSender messageSender;

    @Nested
    @DisplayName("sendBatchErrorToSlack 메소드는")
    class sendBatchErrorToSlack{
        @Test
        @DisplayName("BatchErrorDto를 포맷팅하여 MessageSender로 전송한다.")
        void sendsFormattedMessageToSlack() {
            // given
            BatchErrorDto dto = BatchErrorDto.builder()
                    .jobName("testJob")
                    .summary("Test Error")
                    .build();
            String expectedMessage = dto.formattingMessage();

            // when
            notifyErrorToAdmin.sendBatchErrorToSlack(dto);

            // then
            verify(messageSender).send(expectedMessage);
        }
    }


    @Nested
    @DisplayName("sendMemberScoreIncreaseErrorToSlack 메소드는")
    class sendMemberScoreIncreaseErrorToSlack {

        @Test
        @DisplayName("MemberScoreErrorDto를 포맷팅하여 MessageSender로 전송한다")
        void sendsFormattedMessageToSlack() {
            // given
            MemberScoreErrorDto dto = MemberScoreErrorDto.builder()
                    .serviceName("testService")
                    .summary("Test Score Error")
                    .build();
            String expectedMessage = dto.formattingMessage();

            // when
            notifyErrorToAdmin.sendMemberScoreIncreaseErrorToSlack(dto);

            // then
            verify(messageSender).send(expectedMessage);
        }
    }
}