package com.project200.undabang.alert.service;

public interface AlertService {
    void activateAlert(String fcmToken);
    void deactivateAlert(String fcmToken);
}
