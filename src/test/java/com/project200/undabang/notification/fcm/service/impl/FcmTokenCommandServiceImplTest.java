package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenCommandServiceImpl 테스트")
class FcmTokenCommandServiceImplTest {

    private final UUID testUserId = UUID.randomUUID();
    private final Member member = Member.builder().memberId(testUserId).build();
    private final String fcmTokenValue = "test-fcm-token";
    private final String userAgent = "Test-User-Agent";
    @InjectMocks
    private FcmTokenCommandServiceImpl fcmTokenCommandService;
    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Nested
    @DisplayName("FCM 토큰 저장 기능 테스트")
    class SaveFcmToken {

        @Test
        @DisplayName("새로운 FCM 토큰을 성공적으로 저장한다")
        void saveFcmToken_NewToken_Success() {
            // given
            BDDMockito.given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, testUserId))
                    .willReturn(Optional.empty());

            // when
            fcmTokenCommandService.saveFcmToken(member, fcmTokenValue, userAgent);

            // then
            ArgumentCaptor<FcmToken> fcmTokenCaptor = ArgumentCaptor.forClass(FcmToken.class);
            BDDMockito.then(fcmTokenRepository).should().save(fcmTokenCaptor.capture());

            FcmToken savedToken = fcmTokenCaptor.getValue();
            assertThat(savedToken.getMember()).as("Member 정보가 정확히 저장되어야 합니다.").isEqualTo(member);
            assertThat(savedToken.getFcmTokenValue()).as("FCM 토큰 값이 정확히 저장되어야 합니다.").isEqualTo(fcmTokenValue);
            assertThat(savedToken.getFcmTokenUserAgent()).as("User-Agent 정보가 정확히 저장되어야 합니다.").isEqualTo(userAgent);
            assertThat(savedToken.getFcmTokenIsActive()).as("새 토큰은 활성화 상태여야 합니다.").isTrue();
        }

        @Test
        @DisplayName("이미 존재하는 비활성 FCM 토큰을 활성화한다")
        void saveFcmToken_ExistingInactiveToken_Activates() {
            // given
            FcmToken existingToken = BDDMockito.spy(FcmToken.builder()
                    .member(member)
                    .fcmTokenValue(fcmTokenValue)
                    .fcmTokenUserAgent(userAgent)
                    .build());
            existingToken.deactivate(); // 비활성 상태로 만듦

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, testUserId))
                    .willReturn(Optional.of(existingToken));

            // when
            fcmTokenCommandService.saveFcmToken(member, fcmTokenValue, userAgent);

            // then
            BDDMockito.then(fcmTokenRepository).should(BDDMockito.never()).save(BDDMockito.any(FcmToken.class));
            BDDMockito.then(existingToken).should().activate(); // activate 메소드 호출 검증
            assertThat(existingToken.getFcmTokenIsActive()).as("기존 토큰이 활성화 상태로 변경되어야 합니다.").isTrue();
        }
    }

    @Nested
    @DisplayName("FCM 토큰 비활성화 기능 테스트")
    class DeactivateFcmToken {

        @Test
        @DisplayName("존재하는 FCM 토큰을 성공적으로 비활성화한다")
        void deactivateFcmToken_ExistingToken_Success() {
            // given
            FcmToken existingToken = BDDMockito.spy(FcmToken.builder()
                    .member(member)
                    .fcmTokenValue(fcmTokenValue)
                    .build());

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, testUserId))
                    .willReturn(Optional.of(existingToken));

            // when
            fcmTokenCommandService.deactivateFcmToken(member, fcmTokenValue);

            // then
            BDDMockito.then(existingToken).should().deactivate(); // deactivate 메소드 호출 검증
            assertThat(existingToken.getFcmTokenIsActive()).as("토큰이 비활성화 상태여야 합니다.").isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 FCM 토큰을 비활성화 시도 시 아무 작업도 수행하지 않는다")
        void deactivateFcmToken_NonExistingToken_DoesNothing() {
            // given
            BDDMockito.given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, testUserId))
                    .willReturn(Optional.empty());

            // when
            fcmTokenCommandService.deactivateFcmToken(member, fcmTokenValue);

            // then
            BDDMockito.then(fcmTokenRepository).should(BDDMockito.never()).save(BDDMockito.any(FcmToken.class));
            // 아무런 추가적인 상호작용이 없었는지 확인
            BDDMockito.then(fcmTokenRepository).should().findByFcmTokenValueAndMember_MemberId(fcmTokenValue, testUserId);
            BDDMockito.then(fcmTokenRepository).shouldHaveNoMoreInteractions();
        }
    }
}