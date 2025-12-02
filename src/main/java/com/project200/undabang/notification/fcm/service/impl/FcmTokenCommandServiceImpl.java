package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.auth.dto.request.LoginRequestDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.entity.NotificationType;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepository;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import com.project200.undabang.notification.repository.NotificationTypeRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FcmTokenCommandServiceImpl implements FcmTokenCommandService {

    private final DeviceNotificationSettingRepository deviceNotificationSettingRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final EntityManager em;

    @Override
    public void saveFcmToken(Member member, String fcmToken, String userAgent, LoginRequestDto requestDto) {

        // 토큰이 사용중인지 조사
        Optional<FcmToken> optionalFcmToken = fcmTokenRepository.findByFcmTokenValue(fcmToken);

        if (optionalFcmToken.isEmpty()) {
            createNewFcmToken(member, fcmToken, userAgent, requestDto); // 새로운 토큰 생성
        } else {
            FcmToken existToken = optionalFcmToken.get();
            if (existToken.getMember().getMemberId().equals(member.getMemberId())) {
                existToken.activate(); // 재 로그인 하는 경우
            } else {
                updateTokenOwner(existToken, member, userAgent, requestDto);
            }
        }
    }

    @Override
    public void deactivateFcmToken(Member member, String fcmToken) {
        fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmToken, member.getMemberId())
                .ifPresentOrElse(existingToken -> {
                    log.debug("FCM 토큰을 비활성화합니다. 회원 id: {}", member.getMemberId());
                    existingToken.deactivate();
                }, () -> log.warn("비활성화할 FCM 토큰이 존재하지 않습니다. 회원 id: {}", member.getMemberId()));
    }

    /**
     * 무효한 FCM 토큰을 DB에서 삭제합니다.
     * 이 메소드는 호출될 때마다 새로운 트랜잭션 내에서 실행됩니다.
     *
     * @param tokensToDelete 삭제할 토큰 값 리스트
     */
    @Override
    public void deleteInvalidTokens(List<String> tokensToDelete) {
        if (tokensToDelete == null || tokensToDelete.isEmpty()) {
            return;
        }
        try {
            long deletedCount = fcmTokenRepository.deleteByFcmTokenValueIn(tokensToDelete);
            log.info("[토큰 정리] 총 {}개의 무효 토큰이 성공적으로 삭제되었습니다.", deletedCount);
        } catch (Exception e) {
            log.error("[토큰 정리 실패] 무효 토큰 삭제 중 DB 오류 발생", e);
        }
    }

    /**
     * 새로운 FCM 토큰과 관련 알림 설정을 생성하고 저장합니다.
     */
    private void createNewFcmToken(Member member, String fcmToken, String userAgent, LoginRequestDto requestDto) {
        FcmToken newToken = FcmToken.from(member, fcmToken, userAgent, requestDto);

        // 채팅알림 및 운동 격려 알림 활성화
        createDefaultSettingForFcmToken(newToken);

        fcmTokenRepository.save(newToken);
    }

    /**
     * 기존 FCM 토큰 소유자를 업데이트하고,
     * 새로운 회원 및 사용자 에이전트를 설정하는 메서드입니다.
     * 관련된 기존 알림 설정을 삭제하고, 새로운 기본 알림 설정을 생성합니다.
     */
    private void updateTokenOwner(FcmToken prevFcmToken, Member member, String userAgent, LoginRequestDto requestDto) {
        deviceNotificationSettingRepository.deleteAllByFcmToken(prevFcmToken);
        prevFcmToken.getDeviceNotificationSettingList().clear();

        em.flush();
        em.clear();

        // clear()로 인해 prevFcmToken은 준영속 상태가 되었으므로, 다시 영속성 컨텍스트에 병합
        // merge 후에는 반드시 반환된 'managedToken'을 사용해야 함
        FcmToken managedToken = em.merge(prevFcmToken);

        managedToken.updateOwner(member, userAgent, requestDto);
        createDefaultSettingForFcmToken(managedToken);
    }

    /**
     * 주어진 FCM 토큰에 대해 기본 알림 설정을 생성합니다.
     */
    private void createDefaultSettingForFcmToken(FcmToken fcmToken) {
        List<NotificationType> notificationTypeList = notificationTypeRepository.findAllByDefaultEnabledTrueAndIsActiveTrue();

        List<DeviceNotificationSetting> deviceNotificationSettingList = notificationTypeList.stream()
                .map(notificationType -> DeviceNotificationSetting.of(fcmToken, notificationType))
                .toList();

        fcmToken.getDeviceNotificationSettingList().addAll(deviceNotificationSettingList);
    }
}
