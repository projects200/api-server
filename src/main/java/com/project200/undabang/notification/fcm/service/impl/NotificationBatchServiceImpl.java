package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.notification.fcm.dto.NotificationContent;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.entity.ScenarioCode;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.fcm.repository.NotificationMessageRepository;
import com.project200.undabang.notification.fcm.service.NotificationBatchService;
import com.project200.undabang.notification.fcm.service.NotificationService;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchServiceImpl implements NotificationBatchService {

    private static final int BATCH_SIZE = 500;// 예시 정책 값
    private final FcmTokenRepository fcmTokenRepository;
    private final PolicyService policyService;  // 정책 조회 서비스
    private final NotificationService notificationService; // FCM 메시지를 실제로 보내는 서비스
    private final NotificationMessageRepository notificationMessageRepository;

    // 매일 오후 6시에 실행
    @Scheduled(cron = "0 0 18 * * ?")
    @Override
    @Transactional(readOnly = true)
    public void sendInactivityNotifications() {
        log.info("비활성 회원 알림 배치 작업을 시작합니다.");
        // 점수 감소되고 있는 회원들에게 독려 메시지 발송
        sendPreInactiveNotifications();

        // 점수 감소되기 직전인 회원들에게 독려 메시지 발송
        // 향후 추가

        log.info("비활성 회원 알림 배치 작업을 종료합니다.");
    }

    private void sendPreInactiveNotifications() {
        Page<String> tokenPage;
        int pageNumber = 0;

        int penaltyThresholdDays = policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);

        NotificationContent randomMessageByScenario =
                notificationMessageRepository.findRandomMessageByScenario(ScenarioCode.POST_INACTIVITY_NUDGE);

        // 500개씩 끊어서 토큰 조회
        do {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
            tokenPage = fcmTokenRepository.findFcmTokensForInactiveMembers(
                    penaltyThresholdDays,
                    pageable
            );

            List<String> tokens = tokenPage.getContent();
            if (!tokens.isEmpty()) {
                log.info("{}페이지의 비활성 예고 알림을 전송합니다. 대상 토큰 수: {}", pageNumber, tokens.size());

                List<NotificationPayload> notifications = tokens.stream()
                        .map(token -> new NotificationPayload(
                                token,
                                randomMessageByScenario.title(),
                                randomMessageByScenario.body(),
                                randomMessageByScenario.imageUrl()
                        ))
                        .toList();

                notificationService.sendNotification(notifications);
            }

            ++pageNumber;
        } while (tokenPage.hasNext()); // 다음 페이지가 있는 동안 반복
    }
}
