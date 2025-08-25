package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.fcm.service.NotificationService;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationBatchServiceImpl 클래스")
class NotificationBatchServiceImplTest {

    @InjectMocks
    private NotificationBatchServiceImpl notificationBatchService;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private NotificationService notificationService;

    @Nested
    @DisplayName("sendInactivityNotifications 메소드는")
    class Context_sendInactivityNotifications {

        @Test
        @DisplayName("여러 페이지에 걸쳐 비활성 회원이 존재할 경우, 각 페이지마다 알림을 발송한다")
        void givenMultiplePagesOfInactiveMembers_whenSendNotifications_thenSendsNotificationsPerPage() {
            // given
            int penaltyDays = 14;
            given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(penaltyDays);

            // 첫 번째 페이지 Mock 설정
            Page<String> firstPage = mock(Page.class);
            List<String> firstPageTokens = List.of("token1", "token2");
            given(firstPage.getContent()).willReturn(firstPageTokens);
            given(firstPage.hasNext()).willReturn(true);

            // 두 번째 페이지 Mock 설정
            Page<String> secondPage = mock(Page.class);
            List<String> secondPageTokens = List.of("token3");
            given(secondPage.getContent()).willReturn(secondPageTokens);
            given(secondPage.hasNext()).willReturn(false);

            // Repository가 순차적으로 Mock 페이지를 반환하도록 설정
            given(fcmTokenRepository.findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class)))
                    .willReturn(firstPage, secondPage);

            // when
            notificationBatchService.sendInactivityNotifications();

            // then
            // 정책 값은 한 번만 조회해야 함
            then(policyService).should().getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);
            // Repository는 두 번 호출되어야 함 (페이지 0, 1)
            then(fcmTokenRepository).should(org.mockito.Mockito.times(2)).findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class));

            // NotificationService의 sendNotification도 두 번 호출되어야 함
            ArgumentCaptor<List<NotificationPayload>> payloadCaptor = ArgumentCaptor.forClass(List.class);
            then(notificationService).should(org.mockito.Mockito.times(2)).sendNotification(payloadCaptor.capture());

            // 캡처된 인자 검증
            List<NotificationPayload> firstCallPayloads = payloadCaptor.getAllValues().get(0);
            assertThat(firstCallPayloads).hasSize(2);
            assertThat(firstCallPayloads.get(0).targetUserToken()).isEqualTo("token1");

            List<NotificationPayload> secondCallPayloads = payloadCaptor.getAllValues().get(1);
            assertThat(secondCallPayloads).hasSize(1);
            assertThat(secondCallPayloads.get(0).targetUserToken()).isEqualTo("token3");
        }

        @Test
        @DisplayName("단 한 페이지의 비활성 회원만 존재할 경우, 알림을 한 번만 발송한다")
        void givenSinglePageOfInactiveMembers_whenSendNotifications_thenSendsNotificationsOnce() {
            // given
            int penaltyDays = 14;
            given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(penaltyDays);

            Page<String> singlePage = mock(Page.class);
            List<String> tokens = List.of("token1", "token2", "token3");
            given(singlePage.getContent()).willReturn(tokens);
            given(singlePage.hasNext()).willReturn(false); // 다음 페이지 없음

            given(fcmTokenRepository.findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class)))
                    .willReturn(singlePage);

            // when
            notificationBatchService.sendInactivityNotifications();

            // then
            then(fcmTokenRepository).should().findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class));

            ArgumentCaptor<List<NotificationPayload>> payloadCaptor = ArgumentCaptor.forClass(List.class);
            then(notificationService).should().sendNotification(payloadCaptor.capture());

            assertThat(payloadCaptor.getValue()).hasSize(3);
        }

        @Test
        @DisplayName("비활성 회원이 없을 경우, 알림을 발송하지 않는다")
        void givenNoInactiveMembers_whenSendNotifications_thenDoesNotSendAnyNotification() {
            // given
            int penaltyDays = 14;
            given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(penaltyDays);

            Page<String> emptyPage = mock(Page.class);
            given(emptyPage.getContent()).willReturn(Collections.emptyList()); // 비어있는 리스트
            given(emptyPage.hasNext()).willReturn(false);

            given(fcmTokenRepository.findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when
            notificationBatchService.sendInactivityNotifications();

            // then
            then(fcmTokenRepository).should().findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class));
            // sendNotification은 절대 호출되면 안 됨
            then(notificationService).should(never()).sendNotification(any(List.class));
        }

        @Test
        @DisplayName("첫 페이지가 비어있고 다음 페이지에 데이터가 있을 경우, 두 번째 페이지만 알림을 발송한다")
        void givenEmptyFirstPageAndDataInSecondPage_whenSendNotifications_thenSendsOnlyForSecondPage() {
            // given
            int penaltyDays = 14;
            given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(penaltyDays);

            Page<String> firstPage = mock(Page.class);
            given(firstPage.getContent()).willReturn(Collections.emptyList());
            given(firstPage.hasNext()).willReturn(true);

            Page<String> secondPage = mock(Page.class);
            List<String> secondPageTokens = List.of("token3", "token4");
            given(secondPage.getContent()).willReturn(secondPageTokens);
            given(secondPage.hasNext()).willReturn(false);

            given(fcmTokenRepository.findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class)))
                    .willReturn(firstPage, secondPage);

            // when
            notificationBatchService.sendInactivityNotifications();

            // then
            then(fcmTokenRepository).should(org.mockito.Mockito.times(2)).findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class));

            // 알림 발송은 한 번만 이루어져야 함
            ArgumentCaptor<List<NotificationPayload>> payloadCaptor = ArgumentCaptor.forClass(List.class);
            then(notificationService).should().sendNotification(payloadCaptor.capture());

            // 캡처된 페이로드가 두 번째 페이지의 것인지 확인
            List<NotificationPayload> capturedPayloads = payloadCaptor.getValue();
            assertThat(capturedPayloads).hasSize(2);
            assertThat(capturedPayloads.get(0).targetUserToken()).isEqualTo("token3");
        }
    }
}
