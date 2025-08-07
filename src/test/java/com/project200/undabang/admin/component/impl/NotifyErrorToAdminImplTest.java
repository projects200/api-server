package com.project200.undabang.admin.component.impl;

import com.project200.undabang.admin.entity.dto.BatchErrorDto;
import com.project200.undabang.admin.entity.dto.ErrorLevel;
import com.project200.undabang.admin.entity.dto.MemberScoreErrorDto;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.message.MessageSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.mockito.Mockito.*;

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
            Throwable throwable = new RuntimeException("Test Batch Exception");
            JobExecution mockJobExecution = mock(JobExecution.class);
            JobInstance mockJobInstance = mock(JobInstance.class);

            when(mockJobExecution.getJobInstance()).thenReturn(mockJobInstance);
            when(mockJobInstance.getJobName()).thenReturn("testJob");
            when(mockJobExecution.getJobParameters()).thenReturn(new JobParameters());
            when(mockJobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

            BatchErrorDto dto = BatchErrorDto.of(
                    throwable, "testService", ErrorLevel.ERROR,
                    "Test Error", "test-env", mockJobExecution
            );

            String expectedMessage = dto.formattingMessage();

            // when
            notifyErrorToAdmin.sendErrorToSlackApi(dto);

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
            Throwable throwable = new RuntimeException("Test Score Exception");
            UUID mockUserId = UUID.randomUUID();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/test/uri");
            request.setMethod("GET");

            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class);
                 MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {

                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(mockUserId);
                requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(new ServletRequestAttributes(request));

                MemberScoreErrorDto dto = MemberScoreErrorDto.of(
                        throwable, "testService", ErrorLevel.WARN,
                        "Test Score Error", "test-env"
                );

                String expectedMessage = dto.formattingMessage();

                // when
                notifyErrorToAdmin.sendErrorToSlackApi(dto);

                // then
                verify(messageSender).send(expectedMessage);
            }
        }
    }
}