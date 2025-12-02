package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.auth.dto.request.LoginRequestDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.entity.NotificationType;
import com.project200.undabang.notification.fcm.entity.FcmAccessMode;
import com.project200.undabang.notification.fcm.entity.FcmPlatform;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepository;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.repository.NotificationTypeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.NonTransientDataAccessException;

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

    @InjectMocks
    private FcmTokenCommandServiceImpl fcmTokenCommandService;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    @Mock
    private NotificationTypeRepository notificationTypeRepository;

    @Mock
    private EntityManager em;

    @Nested
    @DisplayName("FCM 토큰 비활성화 기능은")
    class DeactivateFcmToken {

        @Test
        @DisplayName("존재하는 FCM 토큰을 성공적으로 비활성화한다")
        void it_deactivates_an_existing_token_successfully() {
            // given
            Member member = createMember(UUID.randomUUID());
            String fcmTokenValue = "existing-token";
            FcmToken existingToken = createSpyFcmToken(member, fcmTokenValue);

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, member.getMemberId()))
                    .willReturn(Optional.of(existingToken));

            // when
            fcmTokenCommandService.deactivateFcmToken(member, fcmTokenValue);

            // then
            then(existingToken).should().deactivate();
        }

        @Test
        @DisplayName("존재하지 않는 FCM 토큰 비활성화 시도 시 아무 작업도 수행하지 않는다")
        void it_does_nothing_if_token_does_not_exist() {
            // given
            Member member = createMember(UUID.randomUUID());
            String fcmTokenValue = "non-existing-token";
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
    @DisplayName("무효한 FCM 토큰 삭제 기능은")
    class DeleteInvalidTokens {

        @Test
        @DisplayName("토큰 리스트를 받아 성공적으로 삭제 메소드를 호출한다")
        void it_calls_delete_method_with_token_list() {
            // given
            List<String> tokensToDelete = List.of("token1", "token2", "token3");

            // when
            fcmTokenCommandService.deleteInvalidTokens(tokensToDelete);

            // then
            then(fcmTokenRepository).should(times(1)).deleteByFcmTokenValueIn(tokensToDelete);
        }

        @Test
        @DisplayName("빈 리스트나 null을 전달하면 삭제 메소드를 호출하지 않는다")
        void it_does_not_call_delete_method_for_empty_or_null_list() {
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
        void it_does_not_throw_exception_when_db_error_occurs() {
            // given
            List<String> tokensToDelete = List.of("token1");
            DataAccessException dbException = new NonTransientDataAccessException("Test DB Exception") {
            };
            BDDMockito.willThrow(dbException).given(fcmTokenRepository).deleteByFcmTokenValueIn(tokensToDelete);

            // when & then
            assertDoesNotThrow(() -> fcmTokenCommandService.deleteInvalidTokens(tokensToDelete));
            then(fcmTokenRepository).should().deleteByFcmTokenValueIn(tokensToDelete);
        }
    }

    private Member createMember(UUID id) {
        return Member.builder().memberId(id).build();
    }

    private LoginRequestDto createLoginRequestDto() {
        return new LoginRequestDto(FcmPlatform.IOS, FcmAccessMode.APP);
    }

    @Nested
    @DisplayName("FCM 토큰 저장/갱신 기능은")
    class Describe_saveFcmToken {

        @Test
        @DisplayName("새로운 토큰일 경우, DB에서 기본 설정을 조회하여 토큰을 생성하고 저장한다")
        void it_creates_and_saves_new_token_with_default_settings_from_db() {
            // given
            String fcmTokenValue = "new-fcm-token";
            String userAgent = "Test-User-Agent";
            Member member = createMember(UUID.randomUUID());
            LoginRequestDto requestDto = createLoginRequestDto(); // DTO 추가

            NotificationType chatType = NotificationType.builder().id(1L).notificationTypeCode("CHAT_MESSAGE").build();
            NotificationType workoutType = NotificationType.builder().id(2L).notificationTypeCode("WORKOUT_REMINDER").build();
            List<NotificationType> defaultTypes = List.of(chatType, workoutType);

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.empty());
            BDDMockito.given(notificationTypeRepository.findAllByDefaultEnabledTrueAndIsActiveTrue()).willReturn(defaultTypes);

            // when
            // 메서드 호출 시 requestDto 전달
            fcmTokenCommandService.saveFcmToken(member, fcmTokenValue, userAgent, requestDto);

            // then
            ArgumentCaptor<FcmToken> fcmTokenCaptor = ArgumentCaptor.forClass(FcmToken.class);
            then(fcmTokenRepository).should().save(fcmTokenCaptor.capture());

            FcmToken savedToken = fcmTokenCaptor.getValue();
            assertThat(savedToken.getMember()).isEqualTo(member);
            assertThat(savedToken.getFcmTokenValue()).isEqualTo(fcmTokenValue);

            // (필요 시) 저장된 토큰에 DTO의 정보(Platform, AccessMode)가 들어갔는지 검증 로직 추가 가능
            // assertThat(savedToken.getPlatform()).isEqualTo(requestDto.getPlatform());

            assertThat(savedToken.getDeviceNotificationSettingList())
                    .extracting(setting -> setting.getNotificationType().getNotificationTypeCode())
                    .containsExactlyInAnyOrder("CHAT_MESSAGE", "WORKOUT_REMINDER");
        }

        @Test
        @DisplayName("기존 토큰이면서 소유자가 같을 경우, 토큰을 재활성화만 한다")
        void it_reactivates_token_if_owner_is_same() {
            // given
            String fcmTokenValue = "existing-fcm-token";
            String userAgent = "Test-User-Agent";
            Member member = createMember(UUID.randomUUID());
            LoginRequestDto requestDto = createLoginRequestDto(); // DTO 추가
            FcmToken existingToken = createSpyFcmToken(member, fcmTokenValue);

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.of(existingToken));

            // when
            fcmTokenCommandService.saveFcmToken(member, fcmTokenValue, userAgent, requestDto);

            // then
            then(existingToken).should().activate();

            // saveFcmToken 로직상 소유자가 같으면 updateOwner 등을 호출하지 않으므로 DTO 사용 여부는 검증하지 않아도 됨 (혹은 activate 내부 로직에 따라 다름)
            then(fcmTokenRepository).should(BDDMockito.never()).save(any());
            then(notificationTypeRepository).should(BDDMockito.never()).findAllByDefaultEnabledTrueAndIsActiveTrue();
        }

        @Test
        @DisplayName("기존 토큰이면서 소유자가 다를 경우, 소유권을 이전하고 새로운 기본 설정을 생성한다")
        void it_transfers_ownership_and_creates_new_default_settings_if_owner_is_different() {
            // given
            String fcmTokenValue = "shared-fcm-token";
            String userAgent = "Test-User-Agent";
            Member newOwner = createMember(UUID.randomUUID());
            Member oldOwner = createMember(UUID.randomUUID());
            LoginRequestDto requestDto = createLoginRequestDto(); // DTO 추가
            FcmToken existingToken = createSpyFcmToken(oldOwner, fcmTokenValue);

            NotificationType chatType = NotificationType.builder().id(1L).notificationTypeCode("CHAT_MESSAGE").build();
            NotificationType workoutType = NotificationType.builder().id(2L).notificationTypeCode("WORKOUT_REMINDER").build();
            List<NotificationType> defaultTypes = List.of(chatType, workoutType);

            BDDMockito.given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.of(existingToken));
            BDDMockito.given(em.merge(existingToken)).willReturn(existingToken);
            BDDMockito.given(notificationTypeRepository.findAllByDefaultEnabledTrueAndIsActiveTrue()).willReturn(defaultTypes);

            // when
            fcmTokenCommandService.saveFcmToken(newOwner, fcmTokenValue, userAgent, requestDto);

            // then
            InOrder inOrder = inOrder(deviceNotificationSettingRepository, em, existingToken, notificationTypeRepository);

            inOrder.verify(deviceNotificationSettingRepository).deleteAllByFcmToken(existingToken);
            inOrder.verify(existingToken).getDeviceNotificationSettingList(); // clear() 호출 검증을 위한 접근
            inOrder.verify(em).flush();
            inOrder.verify(em).clear();
            inOrder.verify(em).merge(existingToken);

            // [중요] updateOwner 메서드 호출 시 requestDto가 전달되는지 검증
            inOrder.verify(existingToken).updateOwner(newOwner, userAgent, requestDto);

            inOrder.verify(notificationTypeRepository).findAllByDefaultEnabledTrueAndIsActiveTrue();

            // then: 최종 상태를 검증
            assertThat(existingToken.getMember()).isEqualTo(newOwner);
            assertThat(existingToken.getDeviceNotificationSettingList())
                    .extracting(setting -> setting.getNotificationType().getNotificationTypeCode())
                    .containsExactlyInAnyOrder("CHAT_MESSAGE", "WORKOUT_REMINDER");
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
}