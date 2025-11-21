package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.notification.entity.NotificationCode;
import com.project200.undabang.notification.entity.QNotificationType;
import com.project200.undabang.notification.fcm.entity.QDeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.QFcmToken;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepositoryCustom;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FcmTokenRepositoryImpl implements FcmTokenRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<String> findFcmTokensForInactiveMembers(int penaltyThresholdDays, Pageable pageable) {
        QMember member = QMember.member;
        QExercise exercise = QExercise.exercise;
        QFcmToken fcmToken = QFcmToken.fcmToken;
        QDeviceNotificationSetting deviceNotificationSetting = QDeviceNotificationSetting.deviceNotificationSetting;
        QNotificationType notificationType = QNotificationType.notificationType;

        // --- 1. 날짜 계산 로직 ---
        // '오늘' 날짜의 시작 시간(00:00:00)을 기준으로 패널티 기준 시점을 계산합니다.
        LocalDateTime penaltyThresholdDateTime = LocalDate.now().minusDays(penaltyThresholdDays).atStartOfDay();

        // --- 2. COALESCE 로직 구현 ---
        // 마지막 활동일을 계산합니다. (운동 기록이 없으면 가입일로 대체)
        DateTimeExpression<LocalDateTime> lastActivityDate = new CaseBuilder()
                .when(exercise.exerciseCreatedAt.max().isNull())
                .then(member.memberCreatedAt)
                .otherwise(exercise.exerciseCreatedAt.max());

        // --- 3. 기본 쿼리 작성 ---
        JPAQuery<?> baseQuery = queryFactory
                .from(member)
                .leftJoin(exercise).on(
                        exercise.member.eq(member)
                                .and(exercise.exerciseDeletedAt.isNull()))
                .join(fcmToken).on(fcmToken.member.eq(member))
                .join(deviceNotificationSetting).on(deviceNotificationSetting.fcmToken.eq(fcmToken))
                .join(notificationType).on(deviceNotificationSetting.notificationType.eq(notificationType))
                .where(
                        member.memberDeletedAt.isNull(),
                        fcmToken.fcmTokenIsActive.isTrue(),
                        // FCM 토큰이 만료되지 않은 회원만 조회합니다.
                        fcmToken.fcmTokenExpiredAt.goe(LocalDateTime.now()),
                        // 가입일이 패널티 기준 시점보다 이전인 회원만 조회합니다.
                        member.memberCreatedAt.loe(penaltyThresholdDateTime),
                        // 운동 격려 알림을 받기로 설정한 회원에게만 알림 전송
                        deviceNotificationSetting.notificationType.notificationTypeCode.eq(NotificationCode.WORKOUT_REMINDER.getCode()),
                        deviceNotificationSetting.isEnabled.isTrue()
                )
                .groupBy(member.memberId, fcmToken.fcmTokenValue, member.memberCreatedAt)
                // 마지막 활동일이 패널티 기준 시점보다 이전(오래된)인 회원만 필터링합니다.
                .having(lastActivityDate.loe(penaltyThresholdDateTime));

        // --- 4. 컨텐트 조회 및 결과 조회 ---
        List<String> tokens = baseQuery.clone()
                .select(fcmToken.fcmTokenValue)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        log.info("페이징 처리된 FCM 토큰 개수: {}, 페이지 정보: {}", tokens.size(), pageable);

        // --- 5. 전체 카운트 조회 ---
        long total = baseQuery.clone()
                .select(member.memberId) // fcmTokenValue 대신 가벼운 memberId를 조회
                .fetch().size();

        log.info("전체 비활성 회원 수: {}", total);

        // --- 6. Page 객체 생성 ---
        return new PageImpl<>(tokens, pageable, total);
    }
}
