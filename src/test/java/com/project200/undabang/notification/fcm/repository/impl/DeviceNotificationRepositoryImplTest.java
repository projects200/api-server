package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.entity.NotificationType;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepository;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class DeviceNotificationRepositoryImplTest {

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
    @DisplayName("deleteAllByFcmToken 메소드 테스트")
    class DeleteAllByFcmTokenTest {

        @Test
        @DisplayName("특정 FcmToken에 연결된 모든 설정들을 성공적으로 삭제하고 다른 토큰의 설정은 유지한다")
        void shouldDeleteOnlyAssociatedSettings() {
            // given: 엔티티 객체들을 생성 (아직 영속화되지 않음)
            Member member1 = createMember();
            Member member2 = createMember();

            FcmToken tokenToDeleteFor = createFcmToken(member1, "token-to-delete");
            DeviceNotificationSetting setting1 = createSetting(tokenToDeleteFor, NotificationType.CHAT_MESSAGE);
            DeviceNotificationSetting setting2 = createSetting(tokenToDeleteFor, NotificationType.WORKOUT_REMINDER);

            FcmToken tokenToKeepFor = createFcmToken(member2, "token-to-keep");
            DeviceNotificationSetting setting3 = createSetting(tokenToKeepFor, NotificationType.CHAT_MESSAGE);

            // given: 생성한 모든 엔티티를 한번에 영속화하고 DB에 반영
            persistAndFlush(member1, member2, tokenToDeleteFor, setting1, setting2, tokenToKeepFor, setting3);
            em.clear(); // 영속성 컨텍스트를 비워 깨끗한 상태에서 시작

            // given (검증): DB에 총 3개의 설정이 있는지 확인
            assertThat(deviceNotificationSettingRepository.count()).isEqualTo(3);

            // when: 삭제 메소드 실행
            deviceNotificationSettingRepository.deleteAllByFcmToken(tokenToDeleteFor);
            em.flush();
            em.clear();

            // then: 결과 검증
            List<DeviceNotificationSetting> remainingSettings = deviceNotificationSettingRepository.findAll();
            assertThat(remainingSettings).hasSize(1);
            assertThat(remainingSettings.get(0).getFcmToken().getId()).isEqualTo(tokenToKeepFor.getId());
        }

        @Test
        @DisplayName("삭제할 설정이 없는 FcmToken을 전달해도 오류 없이 정상적으로 완료된다")
        void shouldCompleteWithoutError_whenTokenHasNoSettings() {
            // given
            Member member1 = createMember();
            Member member2 = createMember();
            FcmToken tokenWithNoSettings = createFcmToken(member1, "token-no-settings");
            FcmToken tokenWithSettings = createFcmToken(member2, "token-with-settings");
            DeviceNotificationSetting setting = createSetting(tokenWithSettings, NotificationType.CHAT_MESSAGE);

            persistAndFlush(member1, member2, tokenWithNoSettings, tokenWithSettings, setting);
            em.clear();
            assertThat(deviceNotificationSettingRepository.count()).isEqualTo(1);

            // when
            deviceNotificationSettingRepository.deleteAllByFcmToken(tokenWithNoSettings);
            em.flush();
            em.clear();

            // then
            assertThat(deviceNotificationSettingRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("null 값을 인자로 전달하면 예외 없이 완료되고, DB는 변경되지 않는다")
        void shouldDoNothingWithoutException_whenArgumentIsNull() {
            // given: 테스트 데이터 준비
            Member member = createMember();
            FcmToken token = createFcmToken(member, "token-1");
            createSetting(token, NotificationType.CHAT_MESSAGE);
            persistAndFlush(member, token, createSetting(token, NotificationType.CHAT_MESSAGE));
            em.clear();

            long countBefore = deviceNotificationSettingRepository.count();
            assertThat(countBefore).isEqualTo(1);

            // when & then: 예외가 발생하지 않는지 확인
            assertDoesNotThrow(() -> {
                deviceNotificationSettingRepository.deleteAllByFcmToken(null);
                em.flush();
                em.clear();
            });

            // then: DB 상태가 그대로인지 확인
            long countAfter = deviceNotificationSettingRepository.count();
            assertThat(countAfter).isEqualTo(countBefore);
        }
    }
}