package com.project200.undabang.notification.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.dto.record.NotificationSettingRecord;
import com.project200.undabang.notification.dto.request.UpdateDeviceNotificationSettingRequest;
import com.project200.undabang.notification.dto.response.UpdateDeviceNotificationSettingResponse;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.entity.NotificationType;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepository;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSettingCommandServiceImplTest {

    private final UUID memberId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID otherMemberId = UUID.fromString("987e6543-e21b-12d3-a456-426614174999");
    private final String fcmTokenValue = "test-fcm-token";

    @InjectMocks
    private NotificationSettingCommandServiceImpl notificationSettingCommandService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private FcmTokenRepository fcmTokenRepository;
    @Mock
    private DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    private Member createMemberMockOnly(UUID id) {
        return mock(Member.class);
    }

    private Member createMember(UUID id) {
        Member mockMember = mock(Member.class);
        given(mockMember.getMemberId()).willReturn(id);
        return mockMember;
    }

    private FcmToken createFcmToken(Member member) {
        FcmToken mockToken = mock(FcmToken.class);
        given(mockToken.getMember()).willReturn(member);
        return mockToken;
    }

    @Nested
    @DisplayName("디바이스 알림 설정 업데이트 테스트")
    class UpdateDeviceNotificationSettingsTest {

        @Test
        @DisplayName("성공적으로 모든 알림 설정을 업데이트한다")
        void shouldUpdateSettingsSuccessfully() {
            // given
            Member member = createMember(memberId);
            FcmToken fcmToken = createFcmToken(member);

            // 엔티티 자체를 Mocking하여 'updateEnabledStatus' 메소드 호출 여부를 검증합니다.
            DeviceNotificationSetting chatSetting = mock(DeviceNotificationSetting.class);
            given(chatSetting.getNotificationType()).willReturn(NotificationType.CHAT_MESSAGE);
            given(chatSetting.getIsEnabled()).willReturn(true); // 업데이트 후 값

            DeviceNotificationSetting workoutSetting = mock(DeviceNotificationSetting.class);
            given(workoutSetting.getNotificationType()).willReturn(NotificationType.WORKOUT_REMINDER);
            given(workoutSetting.getIsEnabled()).willReturn(false); // 업데이트 후 값

            List<DeviceNotificationSetting> settingList = List.of(chatSetting, workoutSetting);

            List<UpdateDeviceNotificationSettingRequest> requestList = List.of(
                    new UpdateDeviceNotificationSettingRequest(NotificationType.CHAT_MESSAGE, true),
                    new UpdateDeviceNotificationSettingRequest(NotificationType.WORKOUT_REMINDER, false)
            );

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                // Mocking
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.of(fcmToken));
                given(deviceNotificationSettingRepository.findAllByFcmToken(fcmToken)).willReturn(settingList);

                // when
                UpdateDeviceNotificationSettingResponse response = notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, requestList);

                // then
                // 1. 엔티티의 상태 변경 메소드가 올바른 값으로 호출되었는지 검증 (가장 중요)
                verify(chatSetting, times(1)).updateEnabledStatus(true);
                verify(workoutSetting, times(1)).updateEnabledStatus(false);

                // 2. 응답 DTO가 올바르게 생성되었는지 검증
                assertThat(response.getFcmToken()).isEqualTo(fcmTokenValue);
                assertThat(response.getSettings()).hasSize(2);
                assertThat(response.getSettings())
                        .extracting(NotificationSettingRecord::type, NotificationSettingRecord::enabled)
                        .containsExactlyInAnyOrder(
                                tuple(NotificationType.CHAT_MESSAGE, true),
                                tuple(NotificationType.WORKOUT_REMINDER, false)
                        );
            }
        }

        @Test
        @DisplayName("사용자를 찾을 수 없을 경우 CustomException(MEMBER_NOT_FOUND)을 던진다")
        void shouldThrowException_whenMemberNotFound() {
            // given
            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.empty());

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> {
                    notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, List.of());
                });
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

                // then (추가 검증): member 객체가 필요 없으므로 fcmTokenRepository는 호출되지 않아야 함
                verify(fcmTokenRepository, never()).findByFcmTokenValue(anyString());
            }
        }

        @Test
        @DisplayName("FCM 토큰을 찾을 수 없을 경우 CustomException(FCM_TOKEN_NOT_FOUND)을 던진다")
        void shouldThrowException_whenFcmTokenNotFound() {
            Member member = createMemberMockOnly(memberId);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.empty());

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> {
                    notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, List.of());
                });
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FCM_TOKEN_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("FCM 토큰이 다른 사용자의 소유일 경우 CustomException(AUTHORIZATION_DENIED)을 던진다")
        void shouldThrowException_whenFcmTokenBelongsToAnotherUser() {
            Member currentUser = createMember(memberId);
            Member otherUser = createMember(otherMemberId);
            FcmToken tokenOfOtherUser = createFcmToken(otherUser);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(currentUser));
                given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.of(tokenOfOtherUser));

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> {
                    notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, List.of());
                });
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHORIZATION_DENIED);
            }
        }

        @Test
        @DisplayName("요청에 DB에 없는 알림 타입이 포함된 경우 CustomException(DEVICE_NOTIFICATION_SETTING_NOT_FOUND)을 던진다")
        void shouldThrowException_whenSettingNotFoundInDb() {
            // given
            Member member = createMember(memberId);
            FcmToken fcmToken = createFcmToken(member);

            // DB에는 CHAT_MESSAGE 설정만 존재한다고 가정
            DeviceNotificationSetting chatSetting = mock(DeviceNotificationSetting.class);
            given(chatSetting.getNotificationType()).willReturn(NotificationType.CHAT_MESSAGE);
            List<DeviceNotificationSetting> settingListInDb = List.of(chatSetting);

            // 하지만 클라이언트는 WORKOUT_REMINDER 설정 변경을 요청
            List<UpdateDeviceNotificationSettingRequest> requestList = List.of(
                    new UpdateDeviceNotificationSettingRequest(NotificationType.WORKOUT_REMINDER, true)
            );

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.of(fcmToken));
                given(deviceNotificationSettingRepository.findAllByFcmToken(fcmToken)).willReturn(settingListInDb);

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> {
                    notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, requestList);
                });
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DEVICE_NOTIFICATION_SETTING_NOT_FOUND);
            }
        }
    }
}