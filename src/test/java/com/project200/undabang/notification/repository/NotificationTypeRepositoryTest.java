package com.project200.undabang.notification.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.notification.entity.NotificationCategory;
import com.project200.undabang.notification.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class NotificationTypeRepositoryTest {
    @Autowired
    private NotificationTypeRepository notificationTypeRepository;

    @Autowired
    private TestEntityManager em;

    private NotificationType notificationType(String code, boolean defaultEnabled, boolean isActive) {
        return NotificationType.builder()
                .notificationTypeCode(code)
                .category(NotificationCategory.PERSONAL)
                .defaultEnabled(defaultEnabled)
                .isActive(isActive)
                .build();
    }

    private void save(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findAllByDefaultEnabledTrueAndIsActiveTrue 메소드는")
    class Describe_findAllByDefaultEnabledTrueAndIsActiveTrue {

        @Test
        @DisplayName("defaultEnabled와 isActive가 모두 true인 알림 타입만 정확히 조회한다")
        void it_returns_only_types_where_both_defaultEnabled_and_isActive_are_true() {
            // given: 다양한 조건의 NotificationType 엔티티들을 생성
            // 1. 조회 대상
            NotificationType type1 = notificationType("CHAT", true, true);
            NotificationType type2 = notificationType("WORKOUT", true, true);
            // 2. 조회 제외 대상
            NotificationType type3 = notificationType("MARKETING", false, true);
            NotificationType type4 = notificationType("NOTICE", true, false);
            NotificationType type5 = notificationType("EVENT", false, false);

            save(type1, type2, type3, type4, type5);
            flushAndClear();

            // when
            List<NotificationType> foundTypes = notificationTypeRepository.findAllByDefaultEnabledTrueAndIsActiveTrue();

            // then
            assertThat(foundTypes).hasSize(2);
            assertThat(foundTypes)
                    .extracting(NotificationType::getNotificationTypeCode)
                    .containsExactlyInAnyOrder("CHAT", "WORKOUT");
        }

        @Test
        @DisplayName("조건을 만족하는 알림 타입이 하나도 없을 경우 빈 리스트를 반환한다")
        void it_returns_an_empty_list_when_no_types_match_criteria() {
            // given: 조건을 만족하지 않는 엔티티들만 생성
            NotificationType type1 = notificationType("MARKETING", false, true);
            NotificationType type2 = notificationType("NOTICE", true, false);
            NotificationType type3 = notificationType("EVENT", false, false);

            save(type1, type2, type3);
            flushAndClear();

            // when
            List<NotificationType> foundTypes = notificationTypeRepository.findAllByDefaultEnabledTrueAndIsActiveTrue();

            // then
            assertThat(foundTypes).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("DB에 아무 데이터가 없을 경우 빈 리스트를 반환한다")
        void it_returns_an_empty_list_when_table_is_empty() {
            // given: 아무 데이터도 저장하지 않음

            // when
            List<NotificationType> foundTypes = notificationTypeRepository.findAllByDefaultEnabledTrueAndIsActiveTrue();

            // then
            assertThat(foundTypes).isNotNull().isEmpty();
        }
    }
}