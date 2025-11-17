package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.entity.QDeviceNotificationSetting;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DeviceNotificationRepositoryImpl implements DeviceNotificationSettingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 주어진 FCM 토큰을 기준으로 모든 기기 알림 설정 데이터를 삭제합니다.
     */
    @Override
    public void deleteAllByFcmToken(FcmToken fcmToken) {
        QDeviceNotificationSetting setting = QDeviceNotificationSetting.deviceNotificationSetting;

        queryFactory.delete(setting)
                .where(setting.fcmToken.eq(fcmToken))  // fcmToken이 null일 경우 'IS NULL' 조건이 생성되지만, 실제로는 해당 row가 존재하지 않음
                .execute();
    }
}
