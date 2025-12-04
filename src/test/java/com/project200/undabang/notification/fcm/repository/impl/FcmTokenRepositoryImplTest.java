package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.entity.NotificationCategory;
import com.project200.undabang.notification.entity.NotificationCode;
import com.project200.undabang.notification.entity.NotificationType;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmAccessMode;
import com.project200.undabang.notification.fcm.entity.FcmPlatform;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class FcmTokenRepositoryImplTest {

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private TestEntityManager em;

    private Member member(String email, String nickname, LocalDateTime createdAt) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(email)
                .memberNickname(nickname)
                .memberCreatedAt(createdAt)
                .build();
    }

    private FcmToken fcmToken(Member member, String tokenValue, boolean isActive, LocalDateTime expiredAt, FcmPlatform platform, FcmAccessMode mode) {
        return FcmToken.builder()
                .member(member)
                .fcmTokenValue(tokenValue)
                .fcmTokenIsActive(isActive)
                .fcmTokenExpiredAt(expiredAt)
                .fcmPlatform(platform)
                .fcmAccessMode(mode)
                .build();
    }

    @Nested
    @DisplayName("findFcmTokensForInactiveMembers 메소드는")
    class Describe_findFcmTokensForInactiveMembers {

        @Test
        @DisplayName("운동 격려 알림을 활성화한 비활성 대상 중, 유효한 플랫폼(Android-App, iOS-PWA)만 필터링한다")
        void it_filters_only_inactive_members_with_valid_platform_and_settings() {
            // given
            NotificationType workoutType = notificationType("WORKOUT_REMINDER");
            NotificationType chatType = notificationType("CHAT_MESSAGE");
            save(workoutType, chatType);

            int penaltyThresholdDays = 7;
            LocalDateTime now = LocalDateTime.now();

            // 1. [조회 대상] 비활성 + 운동알림 ON + Android APP (Valid)
            Member inactive1 = member("inactive1@test.com", "inactive1", now.minusDays(10));
            FcmToken expectedToken1 = fcmToken(inactive1, "expected-android-app", true, now.plusDays(30), FcmPlatform.ANDROID, FcmAccessMode.APP);
            save(inactive1, exercise(inactive1, now.minusDays(8)), expectedToken1, setting(expectedToken1, workoutType, true));

            // 2. [조회 대상] 비활성(운동기록X) + 운동알림 ON + iOS PWA (Valid)
            Member inactive2 = member("inactive2@test.com", "inactive2", now.minusDays(8));
            FcmToken expectedToken2 = fcmToken(inactive2, "expected-ios-pwa", true, now.plusDays(30), FcmPlatform.IOS, FcmAccessMode.PWA);
            save(inactive2, expectedToken2, setting(expectedToken2, workoutType, true));

            // 3. [제외 대상] 플랫폼 불일치 (WEB / BROWSER)
            Member inactiveWeb = member("web@test.com", "webUser", now.minusDays(10));
            FcmToken webToken = fcmToken(inactiveWeb, "web-token", true, now.plusDays(30), FcmPlatform.PC, FcmAccessMode.BROWSER);
            save(inactiveWeb, webToken, setting(webToken, workoutType, true));

            // 4. [제외 대상] 플랫폼 불일치 (iOS APP - 로직상 iOS는 PWA만 허용됨)
            Member inactiveIosApp = member("iosapp@test.com", "iosAppUser", now.minusDays(10));
            FcmToken iosAppToken = fcmToken(inactiveIosApp, "ios-app-token", true, now.plusDays(30), FcmPlatform.IOS, FcmAccessMode.APP);
            save(inactiveIosApp, iosAppToken, setting(iosAppToken, workoutType, true));

            // 5. [제외 대상] 운동알림 OFF
            Member inactiveOff = member("inactiveOff@test.com", "inactiveOff", now.minusDays(9));
            FcmToken tokenOff = fcmToken(inactiveOff, "token-off", true, now.plusDays(30), FcmPlatform.ANDROID, FcmAccessMode.APP);
            save(inactiveOff, tokenOff, setting(tokenOff, workoutType, false));

            // 6. [제외 대상] 활성 사용자
            Member activeUser = member("active@test.com", "active1", now.minusDays(15));
            FcmToken activeToken = fcmToken(activeUser, "active-token", true, now.plusDays(30), FcmPlatform.ANDROID, FcmAccessMode.APP);
            save(activeUser, exercise(activeUser, now.minusDays(6)), activeToken, setting(activeToken, workoutType, true));

            flushAndClear();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<String> result = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).containsExactlyInAnyOrder(
                    expectedToken1.getFcmTokenValue(),
                    expectedToken2.getFcmTokenValue()
            );
        }

        @Test
        @DisplayName("운동 기록이 없는 비활성 회원이 있어도 SQL 오류 없이 정상 동작한다")
        void it_works_correctly_for_inactive_member_with_no_exercise_record() {
            // given
            NotificationType workoutType = notificationType("WORKOUT_REMINDER");
            save(workoutType);

            int penaltyThresholdDays = 7;
            LocalDateTime now = LocalDateTime.now();

            Member inactiveNoExercise = member("no-exercise@test.com", "no-exercise-user", now.minusDays(8));
            // Default valid platform (Android/APP)
            FcmToken targetToken = fcmToken(inactiveNoExercise, "target-token", true, now.plusDays(30), FcmPlatform.ANDROID, FcmAccessMode.APP);
            save(inactiveNoExercise, targetToken, setting(targetToken, workoutType, true));

            flushAndClear();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<String> result = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).containsExactly(targetToken.getFcmTokenValue());
        }

        @Test
        @DisplayName("다수의 토큰과 설정이 있을 때 페이징이 정확하게 동작한다")
        void it_handles_pagination_correctly() {
            // given
            NotificationType workoutType = notificationType("WORKOUT_REMINDER");
            save(workoutType);

            int penaltyThresholdDays = 7;
            LocalDateTime now = LocalDateTime.now();
            int memberCount = 4;
            int tokensPerMember = 3;
            List<String> allExpectedTokens = new ArrayList<>();

            for (int i = 0; i < memberCount; i++) {
                Member member = member("inactive" + i + "@test.com", "inactive" + i, now.minusDays(8));
                save(member);
                for (int j = 0; j < tokensPerMember; j++) {
                    String tokenValue = "token-" + i + "-" + j;
                    // Default valid platform
                    FcmToken fcmToken = fcmToken(member, tokenValue, true, now.plusDays(30), FcmPlatform.ANDROID, FcmAccessMode.APP);
                    allExpectedTokens.add(tokenValue);
                    save(fcmToken, setting(fcmToken, workoutType, true));
                }
            }
            flushAndClear();

            // when
            Pageable firstPageable = PageRequest.of(0, 5);
            Page<String> firstPage = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, firstPageable);

            Pageable secondPageable = PageRequest.of(1, 5);
            Page<String> secondPage = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, secondPageable);

            // then
            assertThat(firstPage.getTotalElements()).isEqualTo(12);
            assertThat(firstPage.getContent()).hasSize(5);
            assertThat(secondPage.getContent()).hasSize(5);
            assertThat(firstPage.hasNext()).isTrue();
            assertThat(secondPage.hasNext()).isTrue();
        }
    }

    @Nested
    @DisplayName("findAllActivatedFcmTokensForChat 메소드는")
    class Describe_findAllActivatedFcmTokensForChat {

        @Test
        @DisplayName("성공: 채팅 알림 설정이 켜져 있고, 활성화되었으며, 지원하는 플랫폼(Android/App, iOS/PWA)인 토큰만 조회한다")
        void it_returns_active_tokens_with_chat_notification_enabled_and_valid_platform() {
            // given
            NotificationType chatType = notificationType(NotificationCode.CHAT_MESSAGE.getCode());
            NotificationType workoutType = notificationType(NotificationCode.WORKOUT_REMINDER.getCode());
            save(chatType, workoutType);

            Member member = member("test@test.com", "Tester", LocalDateTime.now());
            save(member);

            LocalDateTime future = LocalDateTime.now().plusDays(30);

            // 1. [조회 대상] Android + APP + 채팅 ON
            FcmToken androidAppToken = fcmToken(member, "android_app", true, future, FcmPlatform.ANDROID, FcmAccessMode.APP);
            save(androidAppToken, setting(androidAppToken, chatType, true));

            // 2. [조회 대상] iOS + PWA + 채팅 ON
            FcmToken iosPwaToken = fcmToken(member, "ios_pwa", true, future, FcmPlatform.IOS, FcmAccessMode.PWA);
            save(iosPwaToken, setting(iosPwaToken, chatType, true));

            // 3. [제외 대상] iOS + APP (지원하지 않는 조합)
            FcmToken iosAppToken = fcmToken(member, "ios_app", true, future, FcmPlatform.IOS, FcmAccessMode.APP);
            save(iosAppToken, setting(iosAppToken, chatType, true));

            // 4. [제외 대상] Web + Browser (지원하지 않는 조합)
            FcmToken webToken = fcmToken(member, "web_browser", true, future, FcmPlatform.PC, FcmAccessMode.BROWSER);
            save(webToken, setting(webToken, chatType, true));

            // 5. [제외 대상] 채팅 알림 OFF
            FcmToken chatDisabledToken = fcmToken(member, "chat_disabled", true, future, FcmPlatform.ANDROID, FcmAccessMode.APP);
            save(chatDisabledToken, setting(chatDisabledToken, chatType, false));

            // 6. [제외 대상] 비활성 토큰
            FcmToken inactiveToken = fcmToken(member, "inactive_token", false, future, FcmPlatform.ANDROID, FcmAccessMode.APP);
            save(inactiveToken, setting(inactiveToken, chatType, true));

            flushAndClear();

            // when
            List<String> result = fcmTokenRepository.findAllActivatedFcmTokensForChat(member.getMemberId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder("android_app", "ios_pwa");
        }

        @Test
        @DisplayName("성공: 해당 회원의 유효한 토큰이 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_valid_tokens() {
            // given
            NotificationType chatType = notificationType(NotificationCode.CHAT_MESSAGE.getCode());
            save(chatType);

            Member member = member("test@test.com", "Tester", LocalDateTime.now());
            save(member);

            // 토큰이 아예 없거나 유효한 토큰이 없는 상황
            FcmToken inactiveToken = fcmToken(member, "inactive_token", false, LocalDateTime.now().plusDays(30), FcmPlatform.ANDROID, FcmAccessMode.APP);
            save(inactiveToken, setting(inactiveToken, chatType, true));

            flushAndClear();

            // when
            List<String> result = fcmTokenRepository.findAllActivatedFcmTokensForChat(member.getMemberId());

            // then
            assertThat(result).isEmpty();
        }
    }

    private Exercise exercise(Member member, LocalDateTime createdAt) {
        return Exercise.builder()
                .member(member)
                .exerciseTitle("Sample Test Exercise")
                .exerciseCreatedAt(createdAt)
                .build();
    }

    @Nested
    @DisplayName("findAllExpiredTokenIdList 메소드는")
    class Describe_findAllExpiredTokenIdList {

        @Test
        @DisplayName("오늘 00:00 이전에 만료된 토큰의 ID만 정확히 조회한다")
        void it_returns_only_ids_expired_before_today_midnight() {
            // given
            Member member = member("expire@test.com", "ExpireUser", LocalDateTime.now());
            save(member);

            LocalDateTime todayMidnight = LocalDate.now().atStartOfDay();

            // 플랫폼 정보는 만료 조회 로직에 영향이 없지만, 필수 필드 채움을 위해 기본값 사용
            // 1. [조회 대상] 어제 만료된 토큰
            FcmToken expiredYesterday = fcmToken(member, "expired_yesterday", true, todayMidnight.minusDays(1), FcmPlatform.ANDROID, FcmAccessMode.APP);
            // 2. [조회 대상] 오늘 00:00:00 직전(1초 전)에 만료된 토큰
            FcmToken expiredJustBefore = fcmToken(member, "expired_just_before", true, todayMidnight.minusSeconds(1), FcmPlatform.ANDROID, FcmAccessMode.APP);

            // 3. [제외 대상] 오늘 00:00:00 정각에 만료되는 토큰
            FcmToken exactMidnight = fcmToken(member, "exact_midnight", true, todayMidnight, FcmPlatform.ANDROID, FcmAccessMode.APP);
            // 4. [제외 대상] 내일 만료되는 토큰
            FcmToken validTomorrow = fcmToken(member, "valid_tomorrow", true, todayMidnight.plusDays(1), FcmPlatform.ANDROID, FcmAccessMode.APP);

            save(expiredYesterday, expiredJustBefore, exactMidnight, validTomorrow);
            flushAndClear();

            // when
            List<Long> result = fcmTokenRepository.findAllExpiredTokenIdList(100);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(
                    expiredYesterday.getId(),
                    expiredJustBefore.getId()
            );
            assertThat(result).doesNotContain(
                    exactMidnight.getId(),
                    validTomorrow.getId()
            );
        }
    }

    private NotificationType notificationType(String code) {
        return NotificationType.builder().notificationTypeCode(code).category(NotificationCategory.PERSONAL).build();
    }

    private DeviceNotificationSetting setting(FcmToken fcmToken, NotificationType type, boolean isEnabled) {
        return DeviceNotificationSetting.builder().fcmToken(fcmToken).notificationType(type).isEnabled(isEnabled).build();
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
}