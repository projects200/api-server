package com.project200.undabang.alert.service;

public interface AlertCommandService {
    void activateAlert(String fcmToken);
    void deactivateAlert(String fcmToken);
}
