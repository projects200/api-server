package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.notification.fcm.dto.NotificationContent;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.entity.ScenarioCode;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.fcm.repository.NotificationMessageRepository;
import com.project200.undabang.notification.fcm.service.NotificationService;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@DisplayName("NotificationBatchServiceImpl 단위 테스트")
@ExtendWith(MockitoExtension.class)
class NotificationBatchServiceImplTest {

    @InjectMocks
    private NotificationBatchServiceImpl notificationBatchService;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMessageRepository notificationMessageRepository;

    @Captor
    private ArgumentCaptor<List<NotificationPayload>> payloadListCaptor;

    private List<String> createTokenList(int start, int count) {
        return IntStream.range(start, start + count)
                .mapToObj(i -> "token-" + i)
                .collect(Collectors.toList());
    }

    // --- Test Helper Methods ---

    @Nested
    @DisplayName("sendInactivityNotifications 메소드는")
    class SendInactivityNotifications {

        @Test
        @DisplayName("여러 페이지에 걸쳐 비활성 회원이 존재할 경우, 모든 페이지에 대해 알림을 발송한다")
        void sendNotifications_whenMultiplePagesOfInactiveMembersExist() {
            // given
            int penaltyDays = 14;
            NotificationContent message = new NotificationContent("비활성 알림", "다시 활동해주세요!", null);

            // 첫 번째 페이지 데이터 준비
            List<String> tokensPage1 = createTokenList(0, 500);
            Page<String> page1 = new PageImpl<>(tokensPage1, PageRequest.of(0, 500), 1000); // 총 1000개, 현재 1페이지

            // 두 번째 페이지 데이터 준비
            List<String> tokensPage2 = createTokenList(500, 500);
            Page<String> page2 = new PageImpl<>(tokensPage2, PageRequest.of(1, 500), 1000); // 총 1000개, 현재 2페이지

            // Mock 설정
            given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(penaltyDays);
            given(notificationMessageRepository.findRandomMessageByScenario(ScenarioCode.POST_INACTIVITY_NUDGE)).willReturn(message);
            // 첫 번째 호출 시 page1, 두 번째 호출 시 page2를 반환하도록 설정
            given(fcmTokenRepository.findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class)))
                    .willReturn(page1, page2);

            // when
            notificationBatchService.sendInactivityNotifications();

            // then
            // notificationService의 sendNotification 메소드가 총 2번 호출되었는지 검증
            then(notificationService).should(times(2)).sendNotification(payloadListCaptor.capture());

            // 캡처된 모든 인자(List<NotificationPayload>)를 가져옴
            List<List<NotificationPayload>> allCapturedPayloads = payloadListCaptor.getAllValues();

            // 첫 번째 호출 시 전달된 페이로드 검증
            assertThat(allCapturedPayloads.get(0)).hasSize(500);
            assertThat(allCapturedPayloads.get(0).get(0).targetUserToken()).isEqualTo("token-0");

            // 두 번째 호출 시 전달된 페이로드 검증
            assertThat(allCapturedPayloads.get(1)).hasSize(500);
            assertThat(allCapturedPayloads.get(1).get(0).targetUserToken()).isEqualTo("token-500");
        }

        @Test
        @DisplayName("한 페이지 분량의 비활성 회원만 존재할 경우, 알림을 한번만 발송한다")
        void sendNotifications_whenSinglePageOfInactiveMembersExist() {
            // given
            int penaltyDays = 14;
            NotificationContent message = new NotificationContent("비활성 알림", "다시 활동해주세요!", null);
            List<String> tokens = createTokenList(0, 100);
            Page<String> singlePage = new PageImpl<>(tokens, PageRequest.of(0, 500), 100); // 다음 페이지 없음

            given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(penaltyDays);
            given(notificationMessageRepository.findRandomMessageByScenario(ScenarioCode.POST_INACTIVITY_NUDGE)).willReturn(message);
            given(fcmTokenRepository.findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class))).willReturn(singlePage);

            // when
            notificationBatchService.sendInactivityNotifications();

            // then
            // sendNotification 메소드가 정확히 1번만 호출되었는지 검증
            then(notificationService).should(times(1)).sendNotification(anyList());
        }

        @Test
        @DisplayName("알림을 보낼 비활성 회원이 없을 경우, 알림을 발송하지 않는다")
        void sendNotifications_whenNoInactiveMembersExist() {
            // given
            int penaltyDays = 14;
            given(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).willReturn(penaltyDays);
            given(notificationMessageRepository.findRandomMessageByScenario(ScenarioCode.POST_INACTIVITY_NUDGE))
                    .willReturn(new NotificationContent("제목", "내용", null));
            // 비어있는 페이지를 반환하도록 설정
            given(fcmTokenRepository.findFcmTokensForInactiveMembers(eq(penaltyDays), any(Pageable.class)))
                    .willReturn(Page.empty());

            // when
            notificationBatchService.sendInactivityNotifications();

            // then
            // 알림 발송 서비스가 전혀 호출되지 않았음을 검증
            then(notificationService).should(never()).sendNotification(anyList());
        }
    }
}
