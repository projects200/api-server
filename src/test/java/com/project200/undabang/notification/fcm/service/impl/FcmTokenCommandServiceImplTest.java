package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.entity.NotificationType;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepository;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
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

    @Mock
    private EntityManager em;

    private Member createMember() {
        return Member.builder().memberId(UUID.randomUUID()).build();
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

    private FcmToken createSpyFcmToken(Member owner, String tokenValue) {
        FcmToken token = FcmToken.builder()
                .member(owner)
                .fcmTokenValue(tokenValue)
                .deviceNotificationSettingList(new ArrayList<>())
                .build();
        return BDDMockito.spy(token);
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
            assertThat(savedToken.getDeviceNotificationSettingList()).hasSize(2);
            assertThat(savedToken.getDeviceNotificationSettingList())
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
            then(fcmTokenRepository).should(BDDMockito.never()).save(any());
            then(deviceNotificationSettingRepository).should(BDDMockito.never()).deleteAllByFcmToken(any());
            then(em).should(BDDMockito.never()).flush();
            then(em).should(BDDMockito.never()).clear();

            then(existingToken).should().activate();
        }

        @Test
        @DisplayName("[중요] 다른 사용자의 기존 FCM 토큰 소유권을 이전한다 (QueryDSL + EntityManager 방식 검증)")
        void saveFcmToken_whenDifferentOwner_thenTransfersOwnershipCorrectly() {
            // given
            Member oldOwner = createMember();
            FcmToken existingTokenOfOldOwner = createSpyFcmToken(oldOwner, fcmTokenValue);

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue))
                    .willReturn(Optional.of(existingTokenOfOldOwner));

            // em.merge()가 호출되면, 영속 상태의 토큰(같은 객체)을 반환하도록 설정
            BDDMockito.given(em.merge(existingTokenOfOldOwner)).willReturn(existingTokenOfOldOwner);

            // when
            fcmTokenCommandService.saveFcmToken(member, fcmTokenValue, userAgent);

            // then
            InOrder inOrder = inOrder(deviceNotificationSettingRepository, em, existingTokenOfOldOwner);

            inOrder.verify(deviceNotificationSettingRepository).deleteAllByFcmToken(existingTokenOfOldOwner);
            inOrder.verify(existingTokenOfOldOwner).getDeviceNotificationSettingList(); // clear()를 위해 getter가 호출됨
            inOrder.verify(em).flush();
            inOrder.verify(em).clear();
            inOrder.verify(em).merge(existingTokenOfOldOwner);
            inOrder.verify(existingTokenOfOldOwner).updateOwner(member, userAgent);

            assertThat(existingTokenOfOldOwner.getMember()).isEqualTo(member);
            assertThat(existingTokenOfOldOwner.getDeviceNotificationSettingList()).hasSize(2);
            assertThat(existingTokenOfOldOwner.getDeviceNotificationSettingList())
                    .extracting(DeviceNotificationSetting::getNotificationType)
                    .containsExactlyInAnyOrder(NotificationType.CHAT_MESSAGE, NotificationType.WORKOUT_REMINDER);
        }
    }
}