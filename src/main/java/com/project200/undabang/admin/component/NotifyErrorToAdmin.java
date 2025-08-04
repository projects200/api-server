package com.project200.undabang.admin.component;

import com.project200.undabang.admin.entity.dto.CommonErrorDto;

/**
 * 애플리케이션의 모든 알림 기능을 대표하는 단일 진입점(Facade) 인터페이스입니다.
 * 비즈니스 로직은 이 인터페이스에만 의존하며, 알림의 구체적인 방식(슬랙, SMS 등)은 알 필요가 없습니다.
 * 이를 통해 비즈니스 로직과 알림 인프라 기술 간의 결합도를 낮춥니다.
 */
public interface NotifyErrorToAdmin {
//    void sendBatchErrorToSlack(BatchErrorDto dto);
//    void sendMemberScoreIncreaseErrorToSlack(MemberScoreErrorDto dto);

    void sendErrorToSlackApi(CommonErrorDto dto);
}
