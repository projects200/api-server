package com.project200.undabang.common.notification.command;

/**
 * 애플리케이션의 모든 알림 기능을 대표하는 단일 진입점(Facade) 인터페이스입니다.
 * 비즈니스 로직은 이 인터페이스에만 의존하며, 알림의 구체적인 방식(슬랙, SMS 등)은 알 필요가 없습니다.
 * 이를 통해 비즈니스 로D직과 알림 인프라 기술 간의 결합도를 낮춥니다.
 */
public interface NotificationCommand {

    /**
     * 시스템에서 처리되지 않은 예외나 중요한 오류 발생 시,
     * 개발팀이 인지할 수 있도록 긴급 알림을 보냅니다.
     * (주로 슬랙 채널을 대상으로 합니다.)
     */
    void sendErrorNotification(String message);
}
