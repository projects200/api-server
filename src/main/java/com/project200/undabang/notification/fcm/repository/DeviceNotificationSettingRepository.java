package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceNotificationSettingRepository extends JpaRepository<DeviceNotificationSetting, Long>, DeviceNotificationSettingRepositoryCustom {
}
