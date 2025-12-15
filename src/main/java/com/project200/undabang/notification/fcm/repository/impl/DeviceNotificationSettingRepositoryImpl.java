package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.entity.QDeviceNotificationSetting;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceNotificationSettingRepositoryImpl implements DeviceNotificationSettingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 주어진 FCM 토큰을 기준으로 모든 기기 알림 설정 데이터를 삭제합니다.
     */
    @Override
    public void deleteAllByFcmToken(FcmToken fcmToken) {
        QDeviceNotificationSetting setting = QDeviceNotificationSetting.deviceNotificationSetting;

        if (fcmToken == null) {
            log.warn("FCM Token이 존재하지 않습니다.");
            return;
        }

        queryFactory.delete(setting)
                .where(setting.fcmToken.eq(fcmToken))
                .execute();
    }
}
