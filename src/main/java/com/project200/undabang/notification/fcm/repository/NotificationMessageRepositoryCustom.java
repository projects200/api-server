package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.notification.fcm.dto.NotificationContent;
import com.project200.undabang.notification.fcm.entity.ScenarioCode;

public interface NotificationMessageRepositoryCustom {

    NotificationContent findRandomMessageByScenario(ScenarioCode scenarioCode);
}
