package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceNotificationSettingRepository extends JpaRepository<DeviceNotificationSetting, Long>, DeviceNotificationSettingRepositoryCustom {
    List<DeviceNotificationSetting> findAllByFcmToken(FcmToken fcmToken);
}
