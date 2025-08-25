package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchServiceImpl implements NotificationBatchService {

    private static final int BATCH_SIZE = 500;// 예시 정책 값
    private final FcmTokenRepository fcmTokenRepository;
    private final PolicyService policyService;  // 정책 조회 서비스
    private final NotificationService notificationService; // FCM 메시지를 실제로 보내는 서비스

    // 매일 오후 6시에 실행
    @Scheduled(cron = "0 0 18 * * ?")
    @Override
    public void sendInactivityNotifications() {
        log.info("비활성 회원 알림 배치 작업을 시작합니다.");
        sendPreInactiveNotifications();
        // 필요하다면 점수 감소 로직도 여기에 추가
        log.info("비활성 회원 알림 배치 작업을 종료합니다.");
    }

    private void sendPreInactiveNotifications() {
        Page<String> tokenPage;
        int pageNumber = 0;

        int penaltyThresholdDays = policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);

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
                                "운다방",
                                "잠깐! 소중한 운동 점수가 변동될 수 있어요. 가볍게라도 운동하고 지금의 점수를 지켜볼까요?",
                                null
                        ))
                        .toList();

                notificationService.sendNotification(notifications);
            }

            ++pageNumber;
        } while (tokenPage.hasNext()); // 다음 페이지가 있는 동안 반복
    }
}
