package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.entity.NotificationType;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepository;
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
import org.springframework.dao.DataAccessException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenCommandServiceImpl 테스트")
class FcmTokenCommandServiceImplTest {

    private final Member member = createMember();
    private final String fcmTokenValue = "test-fcm-token";
    private final String userAgent = "Test-User-Agent";

    @InjectMocks
    private FcmTokenCommandServiceImpl fcmTokenCommandService;
    @Mock
    private FcmTokenRepository fcmTokenRepository;
    @Mock
    private DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    private Member createMember() {
        return Member.builder().memberId(UUID.randomUUID()).build();
    }

    private FcmToken createSpyFcmToken(Member owner, String tokenValue) {
        return BDDMockito.spy(
                FcmToken.builder()
                        .member(owner)
                        .fcmTokenValue(tokenValue)
                        .settings(new ArrayList<>()) // NPE 방지
                        .build()
        );
    }

    @Nested
    @DisplayName("FCM 토큰 저장/갱신 기능 테스트")
    class SaveFcmToken {

        @Test
        @DisplayName("새로운 FCM 토큰을 성공적으로 저장하고 기본 설정을 추가한다")
        void saveFcmToken_whenNewToken_thenCreatesAndSaves() {
            // given
            BDDMockito.given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue))
                    .willReturn(Optional.empty());

            // when
            fcmTokenCommandService.saveFcmToken(member, fcmTokenValue, userAgent);

            // then
            ArgumentCaptor<FcmToken> fcmTokenCaptor = ArgumentCaptor.forClass(FcmToken.class);
            then(fcmTokenRepository).should().save(fcmTokenCaptor.capture());

            FcmToken savedToken = fcmTokenCaptor.getValue();
            assertThat(savedToken.getMember()).isEqualTo(member);
            assertThat(savedToken.getFcmTokenValue()).isEqualTo(fcmTokenValue);
            assertThat(savedToken.getSettings()).hasSize(2);
            assertThat(savedToken.getSettings())
                    .extracting(DeviceNotificationSetting::getNotificationType)
                    .containsExactlyInAnyOrder(NotificationType.CHAT_MESSAGE, NotificationType.WORKOUT_REMINDER);
        }

        @Test
        @DisplayName("동일한 사용자의 기존 FCM 토큰을 재활성화한다")
        void saveFcmToken_whenSameOwner_thenActivates() {
            // given
            FcmToken existingToken = createSpyFcmToken(member, fcmTokenValue);
            existingToken.deactivate();

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue))
                    .willReturn(Optional.of(existingToken));

            // when
            fcmTokenCommandService.saveFcmToken(member, fcmTokenValue, userAgent);

            // then
            then(fcmTokenRepository).should(BDDMockito.never()).save(BDDMockito.any());
            then(fcmTokenRepository).should(BDDMockito.never()).delete(BDDMockito.any());
            then(deviceNotificationSettingRepository).should(BDDMockito.never()).deleteAll(BDDMockito.any());
            then(existingToken).should().activate();
        }

        @Test
        @DisplayName("[중요] 다른 사용자의 기존 FCM 토큰 소유권을 이전한다 (UPDATE 방식)")
        void saveFcmToken_whenDifferentOwner_thenTransfersOwnership() {
            // given
            Member oldOwner = createMember();
            FcmToken existingTokenOfOldOwner = createSpyFcmToken(oldOwner, fcmTokenValue);

            List<DeviceNotificationSetting> oldSettings = new ArrayList<>();
            oldSettings.add(DeviceNotificationSetting.builder().id(1L).fcmToken(existingTokenOfOldOwner).build());
            oldSettings.add(DeviceNotificationSetting.builder().id(2L).fcmToken(existingTokenOfOldOwner).build());
            existingTokenOfOldOwner.getSettings().addAll(oldSettings);

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue))
                    .willReturn(Optional.of(existingTokenOfOldOwner));

            final List<DeviceNotificationSetting> capturedSettings = new ArrayList<>();

            // BDDMockito.willAnswer(...)를 사용하여 deleteAll이 호출될 때의 동작을 정의
            BDDMockito.willAnswer(invocation -> {
                // deleteAll 메소드에 전달된 첫 번째 인자(리스트)를 가져옴
                List<DeviceNotificationSetting> settingsToDelete = invocation.getArgument(0);
                // 그 내용물을 우리의 로컬 리스트에 복사함
                capturedSettings.addAll(settingsToDelete);
                return null; // deleteAll은 void이므로 null 반환
            }).given(deviceNotificationSettingRepository).deleteAll(BDDMockito.anyList());


            // when
            fcmTokenCommandService.saveFcmToken(member, fcmTokenValue, userAgent);

            // then
            assertThat(capturedSettings)
                    .extracting(DeviceNotificationSetting::getId)
                    .containsExactlyInAnyOrder(1L, 2L);

            // 나머지 검증은 동일하게 수행
            then(deviceNotificationSettingRepository).should().flush();
            then(existingTokenOfOldOwner).should().updateOwner(member, userAgent);

            assertThat(existingTokenOfOldOwner.getSettings()).hasSize(2);
            assertThat(existingTokenOfOldOwner.getSettings())
                    .allMatch(setting -> setting.getId() == null);
        }
    }

    @Nested
    @DisplayName("FCM 토큰 비활성화 기능 테스트")
    class DeactivateFcmToken {

        @Test
        @DisplayName("존재하는 FCM 토큰을 성공적으로 비활성화한다")
        void deactivateFcmToken_ExistingToken_Success() {
            // given
            FcmToken existingToken = createSpyFcmToken(member, fcmTokenValue);

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, member.getMemberId()))
                    .willReturn(Optional.of(existingToken));

            // when
            fcmTokenCommandService.deactivateFcmToken(member, fcmTokenValue);

            // then
            then(existingToken).should().deactivate();
            assertThat(existingToken.getFcmTokenIsActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 FCM 토큰 비활성화 시도 시 아무 작업도 수행하지 않는다")
        void deactivateFcmToken_NonExistingToken_DoesNothing() {
            // given
            BDDMockito.given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, member.getMemberId()))
                    .willReturn(Optional.empty());

            // when
            fcmTokenCommandService.deactivateFcmToken(member, fcmTokenValue);

            // then
            then(fcmTokenRepository).should().findByFcmTokenValueAndMember_MemberId(fcmTokenValue, member.getMemberId());
            then(fcmTokenRepository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("무효한 FCM 토큰 삭제 기능 테스트")
    class DeleteInvalidTokens {

        @Test
        @DisplayName("토큰 리스트를 받아 성공적으로 삭제 메소드를 호출한다")
        void deleteInvalidTokens_Success() {
            // given
            List<String> tokensToDelete = List.of("token1", "token2", "token3");

            // when
            fcmTokenCommandService.deleteInvalidTokens(tokensToDelete);

            // then
            then(fcmTokenRepository).should(times(1)).deleteByFcmTokenValueIn(tokensToDelete);
        }

        @Test
        @DisplayName("빈/null 리스트를 전달하면 삭제 메소드를 호출하지 않는다")
        void deleteInvalidTokens_EmptyOrNullList_DoesNothing() {
            // given
            List<String> emptyList = Collections.emptyList();
            List<String> nullList = null;

            // when
            fcmTokenCommandService.deleteInvalidTokens(emptyList);
            fcmTokenCommandService.deleteInvalidTokens(nullList);

            // then
            then(fcmTokenRepository).should(BDDMockito.never()).deleteByFcmTokenValueIn(BDDMockito.anyList());
        }

        @Test
        @DisplayName("DB 삭제 중 예외가 발생해도 서비스가 중단되지 않는다")
        void deleteInvalidTokens_whenDbErrorOccurs_doesNotThrowException() {
            // given
            List<String> tokensToDelete = List.of("token1");
            DataAccessException dbException = new DataAccessException("Test DB Exception") {
            };

            BDDMockito.willThrow(dbException)
                    .given(fcmTokenRepository).deleteByFcmTokenValueIn(tokensToDelete);

            // when & then
            assertDoesNotThrow(() -> fcmTokenCommandService.deleteInvalidTokens(tokensToDelete));
            then(fcmTokenRepository).should().deleteByFcmTokenValueIn(tokensToDelete);
        }
    }
}