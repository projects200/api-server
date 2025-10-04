package com.project200.undabang.notification.fcm.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.FcmToken;
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

import java.lang.reflect.Field;
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

    @Nested
    @DisplayName("findFcmTokensForInactiveMembers 메소드")
    class FindFcmTokensForInactiveMembersTest {

        @Test
        @DisplayName("다양한 조건의 사용자가 있을 때 정확한 비활성 대상만 필터링하여 FCM 토큰을 반환한다")
        void shouldReturnTokensOfInactiveMembersOnly() {
            // given
            int penaltyThresholdDays = 7;
            LocalDateTime now = LocalDateTime.now();

            // 시나리오 1: 비활성 사용자 (마지막 운동 기록이 8일) -> 조회 대상
            Member inactiveMemberWithExercise = createAndPersistMember("inactiveWithExercise@test.com", "inactive1", now.minusDays(10));
            createAndPersistExercise(inactiveMemberWithExercise, now.minusDays(8));
            FcmToken expectedToken1 = createAndPersistFcmToken(inactiveMemberWithExercise, "expected-token-1", true, now.plusDays(30));

            // 시나리오 2: 비활성 사용자 (운동 기록 없고, 가입일이 8일 전) -> 조회 대상
            Member inactiveMemberWithoutExercise = createAndPersistMember("inactiveWithoutExercise@test.com", "inactive2", now.minusDays(8));
            FcmToken expectedToken2 = createAndPersistFcmToken(inactiveMemberWithoutExercise, "expected-token-2", true, now.plusDays(30));

            // 시나리오 3: 비활성 사용자 (마지막 운동 기록이 7일 1시간 전) -> 조회 제외(일을 기준으로 점검)
            Member borderlineInactiveMember = createAndPersistMember("borderlineInactiveMember@test.com", "inactive3", now.minusDays(7));
            createAndPersistExercise(borderlineInactiveMember, now.minusDays(7).minusHours(1));
            createAndPersistFcmToken(borderlineInactiveMember, "borderline-token-1", true, now.plusDays(30));

            // 시나리오 4: 활성 사용자 (마지막 운동 기록이 6일 전) -> 조회 제외
            Member activeMember = createAndPersistMember("active@test.com", "active1", now.minusDays(15));
            createAndPersistExercise(activeMember, now.minusDays(6));
            createAndPersistFcmToken(activeMember, "active-token-1", true, now.plusDays(30));

            // 시나리오 5: 탈퇴한 사용자 -> 조회 제외
            Member deletedMember = createAndPersistMember("deleted@test.com", "deleted1", now.minusDays(10));
            try {
                Field deletedAtField = Member.class.getDeclaredField("memberDeletedAt");
                deletedAtField.setAccessible(true);
                deletedAtField.set(deletedMember, now); // 탈퇴 처리
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            em.persist(deletedMember);
            createAndPersistFcmToken(deletedMember, "deleted-token-1", true, now.plusDays(30));

            // 시나리오 6: 토큰이 비활성화된 사용자 -> 조회 제외
            Member memberWithInactiveToken = createAndPersistMember("inactiveToken@test.com", "inactiveToken1", now.minusDays(10));
            createAndPersistFcmToken(memberWithInactiveToken, "inactive-fcm-token-1", false, now.plusDays(30));

            // 시나리오 7: 토큰이 만료된 사용자 -> 조회 제외
            Member memberWithExpiredToken = createAndPersistMember("expiredToken@test.com", "expiredToken1", now.minusDays(10));
            createAndPersistFcmToken(memberWithExpiredToken, "expired-fcm-token-1", true, now.minusDays(1));

            // 시나리오 8: 신규 사용자 (가입일이 1일 전) -> 조회 제외
            Member newMember = createAndPersistMember("new@test.com", "new1", now.minusDays(1));
            createAndPersistFcmToken(newMember, "new-member-token-1", true, now.plusDays(30));

            em.flush();
            em.clear();

            Pageable pageable = PageRequest.of(0, 500);

            // when
            Page<String> result = fcmTokenRepository.findFcmTokensForInactiveMembers(penaltyThresholdDays, pageable);

            // then
            assertThat(result).as("결과 페이지는 null이 아니어야 합니다.").isNotNull();
            assertThat(result.getTotalElements()).as("비활성 사용자 2명의 토큰만 조회되어야 합니다.").isEqualTo(2);
            assertThat(result.getContent()).as("조회된 토큰 목록이 정확해야 합니다.")
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
        @DisplayName("여러 회원이 다수의 활성 토큰을 가질 때 페이징이 정확하게 동작한다")
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
                    createAndPersistFcmToken(member, tokenValue, true, now.plusDays(30));
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
            assertThat(currentPage).as("마지막 페이지 정보는 null이 아니어야 합니다.").isNotNull();
            assertThat(currentPage.getTotalElements()).as("전체 토큰 수는 12개여야 합니다.").isEqualTo(12);
            assertThat(allFetchedTokens.size()).as("페이징을 통해 조회된 전체 토큰 수는 12개여야 합니다.").isEqualTo(12);
            assertThat(allFetchedTokens).as("조회된 모든 토큰이 기대한 토큰 목록과 일치해야 합니다.")
                    .containsExactlyInAnyOrderElementsOf(allExpectedTokens);
        }
    }

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

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("activateAllInactiveTokensByMember 메소드")
    class ActivateTokensTest {

        @Test
        @DisplayName("비활성이고 만료되지 않은 토큰만 활성화된다")
        void activateShouldOnlyAffectInactiveAndNotExpiredTokens() {
            LocalDateTime now = LocalDateTime.now();

            Member target = createAndPersistMember("target@test.com", "target", now.minusDays(10));
            Member other = createAndPersistMember("other@test.com", "other", now.minusDays(10));

            // 대상 멤버: 비활성 & 만료 아님 -> 활성화 대상
            createAndPersistFcmToken(target, "t-activate-1", false, now.plusDays(1));
            // 대상 멤버: 비활성 & 만료됨 -> 대상 아님
            createAndPersistFcmToken(target, "t-activate-expired", false, now.minusDays(1));
            // 대상 멤버: 이미 활성 -> 영향 없음
            createAndPersistFcmToken(target, "t-already-active", true, now.plusDays(1));
            // 다른 멤버: 비활성 & 만료 아님 -> 영향 없음
            createAndPersistFcmToken(other, "other-should-not-change", false, now.plusDays(1));

            flushAndClear();

            long updated = fcmTokenRepository.activateAllInactiveTokensByMember(target);
            flushAndClear();

            // 검증: 업데이트 카운트
            assertThat(updated).isEqualTo(1);

            // DB에서 해당 멤버 토큰 상태 확인
            List<FcmToken> targetTokens = em.createQuery("SELECT ft FROM FcmToken ft WHERE ft.member = :m", FcmToken.class)
                    .setParameter("m", target)
                    .getResultList();

            assertThat(targetTokens).extracting(FcmToken::getFcmTokenValue)
                    .contains("t-activate-1", "t-activate-expired", "t-already-active");

            // 값별 상태 검증
            assertThat(targetTokens.stream()
                    .filter(t -> t.getFcmTokenValue().equals("t-activate-1"))
                    .findFirst().get().getFcmTokenIsActive()).isTrue();

            assertThat(targetTokens.stream()
                    .filter(t -> t.getFcmTokenValue().equals("t-activate-expired"))
                    .findFirst().get().getFcmTokenIsActive()).isFalse();

            assertThat(targetTokens.stream()
                    .filter(t -> t.getFcmTokenValue().equals("t-already-active"))
                    .findFirst().get().getFcmTokenIsActive()).isTrue();

            // 다른 멤버 토큰은 변경되지 않음
            FcmToken otherToken = em.createQuery("SELECT ft FROM FcmToken ft WHERE ft.fcmTokenValue = :v", FcmToken.class)
                    .setParameter("v", "other-should-not-change")
                    .getSingleResult();
            assertThat(otherToken.getFcmTokenIsActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("deactivateAllActiveTokensByMember 메소드")
    class DeactivateTokensTest {

        @Test
        @DisplayName("활성이고 만료되지 않은 토큰만 비활성화된다")
        void deactivateShouldOnlyAffectActiveAndNotExpiredTokens() {
            LocalDateTime now = LocalDateTime.now();

            Member target = createAndPersistMember("target2@test.com", "target2", now.minusDays(10));
            Member other = createAndPersistMember("other2@test.com", "other2", now.minusDays(10));

            // 대상 멤버: 활성 & 만료 아님 -> 비활성화 대상
            createAndPersistFcmToken(target, "t-deactivate-1", true, now.plusDays(1));
            // 대상 멤버: 활성 & 만료됨 -> 대상 아님
            createAndPersistFcmToken(target, "t-deactivate-expired", true, now.minusDays(1));
            // 대상 멤버: 이미 비활성 -> 영향 없음
            createAndPersistFcmToken(target, "t-already-inactive", false, now.plusDays(1));
            // 다른 멤버: 활성 & 만료 아님 -> 영향 없음
            createAndPersistFcmToken(other, "other-not-changed-2", true, now.plusDays(1));

            flushAndClear();

            long updated = fcmTokenRepository.deactivateAllActiveTokensByMember(target);
            flushAndClear();

            // 업데이트 카운트 검증
            assertThat(updated).isEqualTo(1);

            List<FcmToken> targetTokens = em.createQuery("SELECT ft FROM FcmToken ft WHERE ft.member = :m", FcmToken.class)
                    .setParameter("m", target)
                    .getResultList();

            // 상태별 검증
            assertThat(targetTokens.stream().collect(java.util.stream.Collectors.toMap(FcmToken::getFcmTokenValue, FcmToken::getFcmTokenIsActive)))
                    .containsEntry("t-deactivate-1", false)
                    .containsEntry("t-deactivate-expired", true)
                    .containsEntry("t-already-inactive", false);

            // 다른 멤버는 변경되지 않음
            FcmToken otherToken = em.createQuery("SELECT ft FROM FcmToken ft WHERE ft.fcmTokenValue = :v", FcmToken.class)
                    .setParameter("v", "other-not-changed-2")
                    .getSingleResult();
            assertThat(otherToken.getFcmTokenIsActive()).isTrue();
        }
    }
}
