package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.auth.dto.request.LoginRequestDto;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.entity.NotificationCategory;
import com.project200.undabang.notification.entity.NotificationType;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmAccessMode;
import com.project200.undabang.notification.fcm.entity.FcmPlatform;
import com.project200.undabang.notification.fcm.entity.FcmToken;
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

    private Member member() {
        String randomValue = UUID.randomUUID().toString().substring(0, 8);
        return Member.builder().memberId(UUID.randomUUID()).memberEmail("user_" + randomValue + "@test.com").memberNickname("nickname_" + randomValue).build();
    }

    private FcmToken fcmToken(Member member, String tokenValue) {
        // 테스트에서 플랫폼 정보가 중요하지 않다면 임의의 값(예: ANDROID/APP)을 사용합니다.
        LoginRequestDto requestDto = new LoginRequestDto(FcmPlatform.ANDROID, FcmAccessMode.APP);
        return FcmToken.from(member, tokenValue, "test-ua", requestDto);
    }

    private NotificationType notificationType(String code) {
        return NotificationType.builder().notificationTypeCode(code).category(NotificationCategory.PERSONAL).build();
    }

    private DeviceNotificationSetting setting(FcmToken token, NotificationType type) {
        return DeviceNotificationSetting.of(token, type);
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
    @DisplayName("findAllByFcmToken 메소드는")
    class Describe_findAllByFcmToken {

        @Test
        @DisplayName("특정 FcmToken에 연결된 설정들만 정확하게 조회한다")
        void it_returns_only_associated_settings() {
            // given: 이 테스트에 필요한 모든 데이터를 독립적으로 생성 및 영속화
            NotificationType chatType = notificationType("CHAT_MESSAGE");
            NotificationType workoutType = notificationType("WORKOUT_REMINDER");
            save(chatType, workoutType);

            Member member1 = member();
            Member member2 = member();
            save(member1, member2);

            FcmToken targetToken = fcmToken(member1, "target-token");
            FcmToken otherToken = fcmToken(member2, "other-token");
            save(targetToken, otherToken);

            save(setting(targetToken, chatType), setting(targetToken, workoutType), setting(otherToken, chatType));
            flushAndClear();

            // when: 대상 토큰으로 설정 목록 조회
            FcmToken managedTargetToken = em.find(FcmToken.class, targetToken.getId());
            List<DeviceNotificationSetting> foundSettings = deviceNotificationSettingRepository.findAllByFcmToken(managedTargetToken);

            // then: 결과 검증
            assertThat(foundSettings).hasSize(2);
            assertThat(foundSettings)
                    .extracting(s -> s.getNotificationType().getNotificationTypeCode())
                    .containsExactlyInAnyOrder("CHAT_MESSAGE", "WORKOUT_REMINDER");
        }

        @Test
        @DisplayName("설정이 없는 FcmToken을 조회하면 빈 리스트를 반환한다")
        void it_returns_an_empty_list_when_token_has_no_settings() {
            // given: 설정이 없는 토큰만 생성 및 영속화
            Member member = member();
            FcmToken tokenWithNoSettings = fcmToken(member, "token-no-settings");
            save(member, tokenWithNoSettings);
            flushAndClear();

            // when: 설정이 없는 토큰으로 조회
            FcmToken managedToken = em.find(FcmToken.class, tokenWithNoSettings.getId());
            List<DeviceNotificationSetting> foundSettings = deviceNotificationSettingRepository.findAllByFcmToken(managedToken);

            // then: 빈 리스트가 반환되는지 확인
            assertThat(foundSettings).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("null 값을 인자로 전달하면 예외 없이 빈 리스트를 반환한다")
        void it_returns_an_empty_list_when_argument_is_null() {
            // given: 다른 설정에 영향을 받지 않음을 확인하기 위한 데이터
            NotificationType chatType = notificationType("CHAT_MESSAGE");
            Member member = member();
            FcmToken token = fcmToken(member, "some-token");
            save(chatType, member, token, setting(token, chatType));
            flushAndClear();

            // when: null 인자로 메소드 호출
            List<DeviceNotificationSetting> foundSettings = deviceNotificationSettingRepository.findAllByFcmToken(null);

            // then: 결과 검증
            assertThat(foundSettings).isNotNull().isEmpty();
        }
    }
}