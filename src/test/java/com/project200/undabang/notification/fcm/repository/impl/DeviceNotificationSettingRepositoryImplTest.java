package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.auth.dto.request.LoginRequestDto;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.entity.NotificationCategory;
import com.project200.undabang.notification.entity.NotificationType;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmAccessMode;
import com.project200.undabang.notification.fcm.entity.FcmPlatform;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@Import(TestQuerydslConfig.class)
class DeviceNotificationSettingRepositoryImplTest {

    @Autowired
    private DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    @Autowired
    private TestEntityManager em;

    private Member member() {
        String randomValue = UUID.randomUUID().toString().substring(0, 8);
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("user_" + randomValue + "@test.com")
                .memberNickname("nickname_" + randomValue)
                .build();
    }

    private NotificationType notificationType(String code) {
        return NotificationType.builder()
                .notificationTypeCode(code)
                .category(NotificationCategory.PERSONAL)
                .build();
    }

    private FcmToken fcmToken(Member member, String tokenValue) {
        // 테스트에서 플랫폼 정보가 중요하지 않다면 임의의 값(예: ANDROID/APP)을 사용합니다.
        LoginRequestDto requestDto = new LoginRequestDto(FcmPlatform.ANDROID, FcmAccessMode.APP);
        return FcmToken.from(member, tokenValue, "test-ua", requestDto);
    }

    private DeviceNotificationSetting setting(FcmToken token, NotificationType type) {
        return DeviceNotificationSetting.of(token, type);
    }

    private void saveAndFlush(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("deleteAllByFcmToken 메소드는")
    class Describe_deleteAllByFcmToken {

        @Test
        @DisplayName("특정 FcmToken에 연결된 모든 설정들을 성공적으로 삭제하고, 다른 토큰의 설정은 유지한다")
        void it_deletes_only_associated_settings_and_keeps_others() {
            // given: 테스트에 필요한 객체들을 생성합니다. (메모리상에만 존재)
            NotificationType chatType = notificationType("CHAT_MESSAGE");
            NotificationType workoutType = notificationType("WORKOUT_REMINDER");

            Member member1 = member();
            Member member2 = member();

            FcmToken tokenToDeleteFor = fcmToken(member1, "token-to-delete");
            FcmToken tokenToKeepFor = fcmToken(member2, "token-to-keep");

            DeviceNotificationSetting setting1 = setting(tokenToDeleteFor, chatType);
            DeviceNotificationSetting setting2 = setting(tokenToDeleteFor, workoutType);
            DeviceNotificationSetting setting3 = setting(tokenToKeepFor, chatType);

            // given: 생성된 모든 객체들을 DB에 저장합니다.
            saveAndFlush(chatType, workoutType, member1, member2, tokenToDeleteFor, tokenToKeepFor, setting1, setting2, setting3);
            flushAndClear();

            // when: 삭제 메소드를 실행합니다.
            FcmToken managedTokenToDelete = em.find(FcmToken.class, tokenToDeleteFor.getId());
            deviceNotificationSettingRepository.deleteAllByFcmToken(managedTokenToDelete);
            flushAndClear();

            // then: 결과를 검증합니다.
            List<DeviceNotificationSetting> remainingSettings = deviceNotificationSettingRepository.findAll();
            assertThat(remainingSettings).hasSize(1);
            assertThat(remainingSettings.get(0).getFcmToken().getId()).isEqualTo(tokenToKeepFor.getId());
        }

        @Test
        @DisplayName("설정이 없는 FcmToken을 전달해도 오류 없이 정상적으로 완료된다")
        void it_completes_without_error_when_token_has_no_settings() {
            // given
            NotificationType chatType = notificationType("CHAT_MESSAGE");
            Member member1 = member();
            Member member2 = member();
            FcmToken tokenWithNoSettings = fcmToken(member1, "token-no-settings");
            FcmToken tokenWithSettings = fcmToken(member2, "token-with-settings");
            DeviceNotificationSetting setting = setting(tokenWithSettings, chatType);

            saveAndFlush(chatType, member1, member2, tokenWithNoSettings, tokenWithSettings, setting);
            flushAndClear();

            long countBefore = deviceNotificationSettingRepository.count();

            // when
            FcmToken managedToken = em.find(FcmToken.class, tokenWithNoSettings.getId());
            deviceNotificationSettingRepository.deleteAllByFcmToken(managedToken);
            flushAndClear();

            // then
            assertThat(deviceNotificationSettingRepository.count()).isEqualTo(countBefore);
        }

        @Test
        @DisplayName("null 값을 인자로 전달하면 예외 없이 완료되고 DB는 변경되지 않는다")
        void it_does_nothing_without_exception_when_argument_is_null() {
            // given
            NotificationType chatType = notificationType("CHAT_MESSAGE");
            Member member = member();
            FcmToken token = fcmToken(member, "token-1");
            DeviceNotificationSetting setting = setting(token, chatType);

            saveAndFlush(chatType, member, token, setting);
            flushAndClear();

            long countBefore = deviceNotificationSettingRepository.count();

            // when & then
            assertDoesNotThrow(() -> {
                deviceNotificationSettingRepository.deleteAllByFcmToken(null);
                flushAndClear();
            });

            // then
            assertThat(deviceNotificationSettingRepository.count()).isEqualTo(countBefore);
        }
    }
}