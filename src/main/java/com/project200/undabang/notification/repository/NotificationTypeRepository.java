package com.project200.undabang.notification.repository;

import com.project200.undabang.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {
    List<NotificationType> findAllByDefaultEnabledTrueAndIsActiveTrue();
}
