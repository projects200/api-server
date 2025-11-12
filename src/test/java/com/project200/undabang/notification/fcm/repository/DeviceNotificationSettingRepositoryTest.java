package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class DeviceNotificationSettingRepositoryTest {

    @Autowired
    private DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    @Autowired
    private TestEntityManager em;

    private void persistAndFlush(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
    }

    private Member createMember() {
        String randomValue = UUID.randomUUID().toString().substring(0, 8);
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("user_" + randomValue + "@test.com")
                .memberNickname("nickname_" + randomValue)
                .build();
    }

    private FcmToken createFcmToken(Member member, String tokenValue) {
        return FcmToken.from(member, tokenValue, "test-ua");
    }

    private DeviceNotificationSetting createSetting(FcmToken token, NotificationType type) {
        return DeviceNotificationSetting.of(token, type);
    }

    @Nested
    @DisplayName("findAllByFcmToken 메소드 테스트")
    class FindAllByFcmTokenTest {

        @Test
        @DisplayName("특정 FcmToken에 연결된 설정들만 정확하게 조회한다")
        void shouldReturnOnlyAssociatedSettings() {
            // given: 2개의 토큰과 각각의 설정을 생성 및 영속화
            Member member1 = createMember();
            Member member2 = createMember();
            FcmToken targetToken = createFcmToken(member1, "target-token");
            FcmToken otherToken = createFcmToken(member2, "other-token");

            DeviceNotificationSetting setting1 = createSetting(targetToken, NotificationType.CHAT_MESSAGE);
            DeviceNotificationSetting setting2 = createSetting(targetToken, NotificationType.WORKOUT_REMINDER);
            DeviceNotificationSetting otherSetting = createSetting(otherToken, NotificationType.CHAT_MESSAGE);

            persistAndFlush(member1, member2, targetToken, otherToken, setting1, setting2, otherSetting);
            em.clear(); // 1차 캐시를 비워 DB에서 직접 조회하도록 보장

            // when: 대상 토큰으로 설정 목록 조회
            List<DeviceNotificationSetting> foundSettings = deviceNotificationSettingRepository.findAllByFcmToken(targetToken);

            // then: 결과 검증
            assertThat(foundSettings).hasSize(2);
            assertThat(foundSettings)
                    .extracting(DeviceNotificationSetting::getNotificationType)
                    .containsExactlyInAnyOrder(NotificationType.CHAT_MESSAGE, NotificationType.WORKOUT_REMINDER);
        }

        @Test
        @DisplayName("설정이 없는 FcmToken을 조회하면 빈 리스트를 반환한다")
        void shouldReturnEmptyList_whenTokenHasNoSettings() {
            // given: 설정이 없는 토큰을 생성 및 영속화
            Member member = createMember();
            FcmToken tokenWithNoSettings = createFcmToken(member, "token-no-settings");

            persistAndFlush(member, tokenWithNoSettings);
            em.clear();

            // when: 설정이 없는 토큰으로 조회
            List<DeviceNotificationSetting> foundSettings = deviceNotificationSettingRepository.findAllByFcmToken(tokenWithNoSettings);

            // then: 빈 리스트가 반환되는지 확인
            assertThat(foundSettings).isNotNull();
            assertThat(foundSettings).isEmpty();
        }

        @Test
        @DisplayName("null 값을 인자로 전달하면 예외 없이 빈 리스트를 반환한다")
        void shouldReturnEmptyList_whenArgumentIsNull() {
            // given: 테스트 데이터 준비 (다른 설정에 영향을 받지 않음을 확인하기 위함)
            Member member = createMember();
            FcmToken token = createFcmToken(member, "some-token");
            DeviceNotificationSetting setting = createSetting(token, NotificationType.CHAT_MESSAGE);
            persistAndFlush(member, token, setting);
            em.clear();

            // when: null 인자로 메소드 호출
            List<DeviceNotificationSetting> foundSettings = deviceNotificationSettingRepository.findAllByFcmToken(null);

            // then: 결과 검증
            assertThat(foundSettings).isNotNull();
            assertThat(foundSettings).isEmpty();
        }
    }
}