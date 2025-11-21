package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.entity.NotificationCategory;
import com.project200.undabang.notification.entity.NotificationType;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
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
        return Member.builder().memberId(UUID.randomUUID()).memberEmail(email).memberNickname(nickname).memberCreatedAt(createdAt).build();
    }

    private Exercise exercise(Member member, LocalDateTime createdAt) {
        return Exercise.builder()
                .member(member)
                .exerciseTitle("Sample Test Exercise") // <--- 이 필드를 추가해야 합니다.
                .exerciseCreatedAt(createdAt)
                .build();
    }

    private FcmToken fcmToken(Member member, String tokenValue, boolean isActive, LocalDateTime expiredAt) {
        return FcmToken.builder().member(member).fcmTokenValue(tokenValue).fcmTokenIsActive(isActive).fcmTokenExpiredAt(expiredAt).build();
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

    @Nested
    @DisplayName("findFcmTokensForInactiveMembers 메소드는")
    class Describe_findFcmTokensForInactiveMembers {

        @Test
        @DisplayName("다양한 조건의 사용자가 있을 때 운동 격려 알림을 활성화한 비활성 대상만 정확히 필터링한다")
        void it_filters_only_inactive_members_with_workout_reminder_enabled() {
            // given: 이 테스트에 필요한 모든 데이터를 독립적으로 생성
            NotificationType workoutType = notificationType("WORKOUT_REMINDER");
            NotificationType chatType = notificationType("CHAT_MESSAGE");
            save(workoutType, chatType);

            int penaltyThresholdDays = 7;
            LocalDateTime now = LocalDateTime.now();

            // 시나리오 1: 비활성 + 운동알림 ON -> 조회 대상
            Member inactive1 = member("inactive1@test.com", "inactive1", now.minusDays(10));
            FcmToken expectedToken1 = fcmToken(inactive1, "expected-token-1", true, now.plusDays(30));
            save(inactive1, exercise(inactive1, now.minusDays(8)), expectedToken1, setting(expectedToken1, workoutType, true));

            // 시나리오 2: 비활성(운동기록X) + 운동알림 ON -> 조회 대상
            Member inactive2 = member("inactive2@test.com", "inactive2", now.minusDays(8));
            FcmToken expectedToken2 = fcmToken(inactive2, "expected-token-2", true, now.plusDays(30));
            save(inactive2, expectedToken2, setting(expectedToken2, workoutType, true));

            // 시나리오 3: 비활성 + 운동알림 OFF -> 조회 제외
            Member inactive3 = member("inactive3@test.com", "inactive3", now.minusDays(9));
            FcmToken token3 = fcmToken(inactive3, "token3", true, now.plusDays(30));
            save(inactive3, token3, setting(token3, workoutType, false));

            // 시나리오 4: 비활성 + 다른알림(채팅) ON -> 조회 제외
            Member inactive4 = member("inactive4@test.com", "inactive4", now.minusDays(9));
            FcmToken token4 = fcmToken(inactive4, "token4", true, now.plusDays(30));
            save(inactive4, token4, setting(token4, chatType, true));

            // 시나리오 5: 활성 사용자 + 운동알림 ON -> 조회 제외
            Member activeUser = member("active@test.com", "active1", now.minusDays(15));
            FcmToken token5 = fcmToken(activeUser, "token5", true, now.plusDays(30));
            save(activeUser, exercise(activeUser, now.minusDays(6)), token5, setting(token5, workoutType, true));

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
            FcmToken targetToken = fcmToken(inactiveNoExercise, "target-token", true, now.plusDays(30));
            save(inactiveNoExercise, targetToken, setting(targetToken, workoutType, true));

            flushAndClear();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<String> result = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, pageable);

            // then
            assertThat(result.getTotalElements()).as("운동 기록 없는 비활성 사용자 1명이 조회되어야 합니다.").isEqualTo(1);
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
                    FcmToken fcmToken = fcmToken(member, tokenValue, true, now.plusDays(30));
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
}
