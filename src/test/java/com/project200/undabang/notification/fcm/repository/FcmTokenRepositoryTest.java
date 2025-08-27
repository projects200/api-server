package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
@DisplayName("FcmTokenRepository 테스트")
class FcmTokenRepositoryTest {

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private EntityManager em;

    @Nested
    @DisplayName("findByFcmTokenValueAndMember_MemberId 메소드 테스트")
    class FindByFcmTokenAndMemberId {

        @Test
        @DisplayName("FCM 토큰과 회원 ID로 토큰 조회 성공")
        void findByFcmTokenValueAndMember_MemberId_Success() {
            // given
            FcmToken persistedToken = persistMemberAndFcmToken();
            String fcmTokenValue = persistedToken.getFcmTokenValue();
            UUID memberId = persistedToken.getMember().getMemberId();

            // when
            Optional<FcmToken> foundTokenOpt = fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, memberId);

            // then
            assertThat(foundTokenOpt).as("FCM 토큰을 찾아야 합니다.").isPresent();
            FcmToken foundToken = foundTokenOpt.get();
            assertThat(foundToken.getId()).as("찾은 토큰의 ID가 일치해야 합니다.").isEqualTo(persistedToken.getId());
            assertThat(foundToken.getFcmTokenValue()).as("찾은 토큰의 값이 일치해야 합니다.").isEqualTo(fcmTokenValue);
            assertThat(foundToken.getMember().getMemberId()).as("찾은 토큰의 회원 ID가 일치해야 합니다.").isEqualTo(memberId);
        }

        @Test
        @DisplayName("FCM 토큰이 일치하지 않으면 토큰을 찾지 못해야 한다")
        void findByFcmTokenValueAndMember_MemberId_Fail_WrongToken() {
            // given
            FcmToken persistedToken = persistMemberAndFcmToken();
            UUID memberId = persistedToken.getMember().getMemberId();
            String wrongFcmToken = "wrong-fcm-token";

            // when
            Optional<FcmToken> foundToken = fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(wrongFcmToken, memberId);

            // then
            assertThat(foundToken).as("잘못된 FCM 토큰 값으로는 토큰을 찾을 수 없어야 합니다.").isNotPresent();
        }

        @Test
        @DisplayName("회원 ID가 일치하지 않으면 토큰을 찾지 못해야 한다")
        void findByFcmTokenValueAndMember_MemberId_Fail_WrongMemberId() {
            // given
            FcmToken persistedToken = persistMemberAndFcmToken();
            String fcmTokenValue = persistedToken.getFcmTokenValue();
            UUID wrongUserId = UUID.randomUUID();

            // when
            Optional<FcmToken> foundToken = fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, wrongUserId);

            // then
            assertThat(foundToken).as("다른 회원의 ID로는 토큰을 찾을 수 없어야 합니다.").isNotPresent();
        }


        @Test
        @DisplayName("FCM 토큰과 회원 ID가 모두 일치하지 않으면 토큰을 찾지 못해야 한다")
        void findByFcmTokenValueAndMember_MemberId_Fail_BothWrong() {
            // given
            persistMemberAndFcmToken();
            String wrongFcmToken = "wrong-fcm-token";
            UUID wrongUserId = UUID.randomUUID();

            // when
            Optional<FcmToken> foundToken = fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(wrongFcmToken, wrongUserId);

            // then
            assertThat(foundToken).as("FCM 토큰과 회원 ID가 모두 틀리면 토큰을 찾을 수 없어야 합니다.").isNotPresent();
        }

        /**
         * 테스트 데이터 생성을 위한 헬퍼 메소드.
         *
         * @return 생성된 FcmToken 엔티티
         */
        private FcmToken persistMemberAndFcmToken() {
            Member member = Member.builder()
                    .memberId(UUID.randomUUID())
                    .memberEmail("test@email.com")
                    .memberNickname("test-user")
                    .build();
            em.persist(member);

            FcmToken fcmToken = FcmToken.builder()
                    .member(member)
                    .fcmTokenValue("test-fcm-token")
                    .fcmTokenUserAgent("Test-User-Agent")
                    .build();
            em.persist(fcmToken);

            em.flush();
            em.clear();

            // 테스트에서 사용할 수 있도록 저장된 엔티티를 반환합니다.
            return fcmToken;
        }
    }
}