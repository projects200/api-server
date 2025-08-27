package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.notification.fcm.dto.NotificationContent;
import com.project200.undabang.notification.fcm.entity.QNotificationMessage;
import com.project200.undabang.notification.fcm.entity.QNotificationScenario;
import com.project200.undabang.notification.fcm.entity.QScenarioMessageMapping;
import com.project200.undabang.notification.fcm.entity.ScenarioCode;
import com.project200.undabang.notification.fcm.repository.NotificationMessageRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationMessageRepositoryImpl implements NotificationMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 시나리오에 따른 메시지 중 랜덤으로 1개 조회
    @Override
    public NotificationContent findRandomMessageByScenario(ScenarioCode scenarioCode) {
        QNotificationMessage message = QNotificationMessage.notificationMessage;
        QNotificationScenario scenario = QNotificationScenario.notificationScenario;
        QScenarioMessageMapping mapping = QScenarioMessageMapping.scenarioMessageMapping;

        return queryFactory.select(Projections.constructor(
                        NotificationContent.class,
                        message.messageTitle,
                        message.messageBody,
                        message.messageImageUrl
                ))
                .from(message)
                .join(mapping).on(mapping.message.eq(message))
                .join(scenario).on(mapping.scenario.eq(scenario))
                .where(scenario.scenarioCode.eq(scenarioCode))
                .orderBy(Expressions.numberTemplate(Double.class, "function('rand')").asc())
                .limit(1)
                .fetchOne();
    }
}
