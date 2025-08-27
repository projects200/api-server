package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.notification.fcm.entity.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long>, NotificationMessageRepositoryCustom {
}
