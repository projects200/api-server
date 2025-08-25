package com.project200.undabang.timer.custom.repository.impl;

import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.QCustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class CustomTimerStepRepositoryImpl implements CustomTimerStepRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager em;

    /**
     * 지정된 CustomTimer와 연관된 모든 CustomTimerStep 엔터티를 논리적으로 삭제합니다.
     *
     * @param customTimer 논리적으로 삭제할 CustomTimerStep과 연관된 CustomTimer 객체
     */
    @Override
    public void softDeleteAllByCustomTimer(CustomTimer customTimer) {
        QCustomTimerStep customTimerStep = QCustomTimerStep.customTimerStep;

        // 이전에 작업한 부모 객체의 상태 변경내용을 DB에 저장
        em.flush();

        // 벌크 연산을 통해 DB에 Step 논리적 삭제 저장
        jpaQueryFactory
                .update(customTimerStep)
                .set(customTimerStep.customTimerStepDeletedAt, LocalDateTime.now())
                .where(
                        customTimerStep.customTimer.eq(customTimer),
                        customTimerStep.customTimerStepDeletedAt.isNull()
                )
                .execute();

        // 영속성 컨텍스트 비우기
        em.clear();
    }
}
