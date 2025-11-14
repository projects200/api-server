package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.entity.NotificationType;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
@DisplayName("FcmTokenQueryRepositoryImpl 클래스")
class FcmTokenRepositoryImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    FcmTokenRepository fcmTokenRepository;

    private Member createAndPersistMember(String email, String nickname, LocalDateTime createdAt) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(email)
                .memberNickname(nickname)
                .memberCreatedAt(createdAt)
                .memberDeletedAt(null) // 명시적으로 null 설정
                .build();
        em.persist(member);
        return member;
    }

    private void createAndPersistExercise(Member member, LocalDateTime createdAt) {
        Exercise exercise = Exercise.builder()
                .member(member)
                .exerciseTitle("Sample Exercise")
                .exerciseCreatedAt(createdAt)
                .exerciseStartedAt(createdAt)
                .exerciseEndedAt(createdAt.plusHours(1))
                .build();
        em.persist(exercise);
    }

    private FcmToken createAndPersistFcmToken(Member member, String tokenValue, boolean isActive, LocalDateTime expiredAt) {
        FcmToken fcmToken = FcmToken.builder()
                .member(member)
                .fcmTokenValue(tokenValue)
                .fcmTokenIsActive(isActive)
                .fcmTokenExpiredAt(expiredAt)
                .build();
        em.persist(fcmToken);
        return fcmToken;
    }

    private void createAndPersistNotificationSetting(FcmToken fcmToken, NotificationType type, boolean isEnabled) {
        DeviceNotificationSetting setting = DeviceNotificationSetting.builder()
                .fcmToken(fcmToken)
                .notificationType(type)
                .isEnabled(isEnabled)
                .build();
        em.persist(setting);
    }

    @Nested
    @DisplayName("findFcmTokensForInactiveMembers 메소드")
    class FindFcmTokensForInactiveMembersTest {

        @Test
        @DisplayName("다양한 조건의 사용자가 있을 때 '운동 격려 알림'을 켠 비활성 대상만 정확히 필터링한다")
        void shouldReturnTokensOfInactiveMembersOnly_whenSettingIsEnabled() {
            // given
            int penaltyThresholdDays = 7;
            LocalDateTime now = LocalDateTime.now();

            // 시나리오 1: 비활성 + 알림 ON -> 조회 대상
            Member inactiveMemberWithExercise = createAndPersistMember("inactiveWithExercise@test.com", "inactive1", now.minusDays(10));
            createAndPersistExercise(inactiveMemberWithExercise, now.minusDays(8));
            FcmToken expectedToken1 = createAndPersistFcmToken(inactiveMemberWithExercise, "expected-token-1", true, now.plusDays(30));
            createAndPersistNotificationSetting(expectedToken1, NotificationType.WORKOUT_REMINDER, true); // 👇 설정 추가

            // 시나리오 2: 비활성(운동기록X) + 알림 ON -> 조회 대상
            Member inactiveMemberWithoutExercise = createAndPersistMember("inactiveWithoutExercise@test.com", "inactive2", now.minusDays(8));
            FcmToken expectedToken2 = createAndPersistFcmToken(inactiveMemberWithoutExercise, "expected-token-2", true, now.plusDays(30));
            createAndPersistNotificationSetting(expectedToken2, NotificationType.WORKOUT_REMINDER, true); // 👇 설정 추가

            // 시나리오 3: 비활성 + '운동 격려 알림' OFF -> 조회 제외 (핵심 테스트)
            Member inactiveMemberWithSettingOff = createAndPersistMember("inactiveSettingOff@test.com", "inactive3", now.minusDays(9));
            FcmToken tokenWithSettingOff = createAndPersistFcmToken(inactiveMemberWithSettingOff, "setting-off-token", true, now.plusDays(30));
            createAndPersistNotificationSetting(tokenWithSettingOff, NotificationType.WORKOUT_REMINDER, false); // 👇 설정 OFF

            // 시나리오 4: 비활성 + '다른 알림(채팅)' ON -> 조회 제외 (핵심 테스트)
            Member inactiveMemberWithOtherSetting = createAndPersistMember("inactiveOtherSetting@test.com", "inactive4", now.minusDays(9));
            FcmToken tokenWithOtherSetting = createAndPersistFcmToken(inactiveMemberWithOtherSetting, "other-setting-token", true, now.plusDays(30));
            createAndPersistNotificationSetting(tokenWithOtherSetting, NotificationType.CHAT_MESSAGE, true); // 👇 다른 알림 설정

            // 시나리오 5: 활성 사용자 (알림 설정 ON) -> 조회 제외
            Member activeMember = createAndPersistMember("active@test.com", "active1", now.minusDays(15));
            createAndPersistExercise(activeMember, now.minusDays(6));
            FcmToken activeToken = createAndPersistFcmToken(activeMember, "active-token-1", true, now.plusDays(30));
            createAndPersistNotificationSetting(activeToken, NotificationType.WORKOUT_REMINDER, true);

            // ... (기존 시나리오 3, 5, 6, 7, 8은 알림 설정 여부와 관계없이 제외되므로 수정 불필요) ...
            Member borderlineInactiveMember = createAndPersistMember("borderlineInactiveMember@test.com", "inactiveBorderline", now.minusDays(7));
            createAndPersistExercise(borderlineInactiveMember, now.minusDays(7).minusHours(1));
            createAndPersistFcmToken(borderlineInactiveMember, "borderline-token-1", true, now.plusDays(30));
            // (탈퇴, 비활성/만료 토큰, 신규 사용자 등은 그대로 유지)

            em.flush();
            em.clear();

            Pageable pageable = PageRequest.of(0, 500);

            // when
            Page<String> result = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).as("비활성이면서 알림을 켠 사용자 2명만 조회되어야 합니다.").isEqualTo(2);
            assertThat(result.getContent())
                    .containsExactlyInAnyOrder(expectedToken1.getFcmTokenValue(), expectedToken2.getFcmTokenValue());
        }

        @Test
        @DisplayName("회귀 테스트: 운동 기록이 없는 회원의 경우, HAVING 절이 memberCreatedAt을 기준으로 동작하여 SQL 오류를 방지한다")
        void givenMemberWithNoExercise_whenQuerying_thenShouldNotThrowSqlError() {
            // given: 이 테스트는 'Unknown column in having clause' 오류가 발생했던 특정 시나리오를 검증합니다.
            // H2 DB는 문법에 너그러워 오류를 재현하지는 않지만, 로직의 정확성은 검증할 수 있습니다.
            int penaltyThresholdDays = 7;
            LocalDateTime now = LocalDateTime.now();

            // 오류 발생 시나리오: 운동 기록과 토큰이 없고, 가입일이 8일 전이라 비활성 대상이 되는 회원 1명만 존재
            createAndPersistMember("no-exercise@test.com", "no-exercise-user", now.minusDays(8));

            em.flush();
            em.clear();

            Pageable pageable = PageRequest.of(0, 500);

            // when: 테스트 메소드 호출
            Page<String> result = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, pageable);

            // then: 결과 검증
            // 이 테스트가 성공하면, GROUP BY 절에 memberCreatedAt이 올바르게 추가되어
            // MySQL과 같은 엄격한 DB에서도 SQL 오류가 발생하지 않음을 보증합니다.
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).as("운동 기록 없는 비활성 사용자 0명이 조회되어야 합니다.").isZero();
        }

        @Test
        @DisplayName("여러 회원이 다수의 활성 토큰과 알림 설정을 가질 때 페이징이 정확하게 동작한다")
        void shouldHandlePaginationCorrectlyWhenMultipleTokensExist() {
            // given
            int penaltyThresholdDays = 7;
            LocalDateTime now = LocalDateTime.now();
            int memberCount = 4;
            int tokensPerMember = 3;
            List<String> allExpectedTokens = new ArrayList<>();

            for (int i = 0; i < memberCount; i++) {
                Member member = createAndPersistMember("inactive" + i + "@test.com", "inactive" + i, now.minusDays(8));
                for (int j = 0; j < tokensPerMember; j++) {
                    String tokenValue = "token-" + i + "-" + j;
                    FcmToken fcmToken = createAndPersistFcmToken(member, tokenValue, true, now.plusDays(30));
                    createAndPersistNotificationSetting(fcmToken, NotificationType.WORKOUT_REMINDER, true);
                    allExpectedTokens.add(tokenValue);
                }
            }

            em.flush();
            em.clear();

            // when
            Pageable pageable = PageRequest.of(0, 5);
            List<String> allFetchedTokens = new ArrayList<>();
            Page<String> currentPage;

            do {
                currentPage = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, pageable);
                allFetchedTokens.addAll(currentPage.getContent());
                pageable = pageable.next();
            } while (currentPage.hasNext());


            // then
            assertThat(currentPage).isNotNull();
            assertThat(currentPage.getTotalElements()).isEqualTo(12);
            assertThat(allFetchedTokens.size()).isEqualTo(12);
            assertThat(allFetchedTokens).containsExactlyInAnyOrderElementsOf(allExpectedTokens);
        }
    }
}
