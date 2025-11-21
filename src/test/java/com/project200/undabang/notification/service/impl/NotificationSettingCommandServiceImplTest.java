package com.project200.undabang.notification.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.dto.record.NotificationSettingRecord;
import com.project200.undabang.notification.dto.request.UpdateDeviceNotificationSettingRequest;
import com.project200.undabang.notification.dto.response.UpdateDeviceNotificationSettingResponse;
import com.project200.undabang.notification.entity.NotificationCategory;
import com.project200.undabang.notification.entity.NotificationType;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
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

    @InjectMocks
    private NotificationSettingCommandServiceImpl notificationSettingCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    private Member createMember(UUID id) {
        return Member.builder().memberId(id).build();
    }

    private FcmToken createFcmToken(Member member, String tokenValue) {
        return FcmToken.from(member, tokenValue, "test-ua");
    }

    private NotificationType createNotificationType(String code) {
        return NotificationType.builder().notificationTypeCode(code).category(NotificationCategory.PERSONAL).build();
    }

    @Nested
    @DisplayName("디바이스 알림 설정 업데이트 기능은")
    class Describe_updateDeviceNotificationSettings {

        @Test
        @DisplayName("성공적으로 모든 알림 설정을 업데이트한다")
        void it_updates_all_settings_successfully() {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmTokenValue = "test-fcm-token";

            Member member = createMember(memberId);
            FcmToken fcmToken = createFcmToken(member, fcmTokenValue);

            NotificationType chatType = createNotificationType("CHAT_MESSAGE");
            NotificationType workoutType = createNotificationType("WORKOUT_REMINDER");

            DeviceNotificationSetting chatSetting = DeviceNotificationSetting.of(fcmToken, chatType);
            DeviceNotificationSetting workoutSetting = DeviceNotificationSetting.of(fcmToken, workoutType);
            fcmToken.getDeviceNotificationSettingList().addAll(List.of(chatSetting, workoutSetting));

            List<UpdateDeviceNotificationSettingRequest> requestList = List.of(
                    new UpdateDeviceNotificationSettingRequest("CHAT_MESSAGE", true),
                    new UpdateDeviceNotificationSettingRequest("WORKOUT_REMINDER", false)
            );

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.of(fcmToken));
                given(deviceNotificationSettingRepository.findAllByFcmToken(fcmToken)).willReturn(fcmToken.getDeviceNotificationSettingList());

                // when
                UpdateDeviceNotificationSettingResponse response = notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, requestList);

                // then:
                assertThat(chatSetting.getIsEnabled()).isTrue();
                assertThat(workoutSetting.getIsEnabled()).isFalse();
                assertThat(response.getFcmToken()).isEqualTo(fcmTokenValue);
                assertThat(response.getSettings())
                        .extracting(NotificationSettingRecord::type, NotificationSettingRecord::enabled)
                        .containsExactlyInAnyOrder(
                                tuple("CHAT_MESSAGE", true),
                                tuple("WORKOUT_REMINDER", false)
                        );
            }
        }

        @Test
        @DisplayName("사용자를 찾을 수 없을 경우 CustomException(MEMBER_NOT_FOUND)을 던진다")
        void it_throws_exception_when_member_not_found() {
            // given
            UUID memberId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.empty());

                // when & then
                CustomException exception = assertThrows(CustomException.class, () ->
                        notificationSettingCommandService.updateDeviceNotificationSetting("any-token", List.of())
                );
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
                verify(fcmTokenRepository, never()).findByFcmTokenValue(anyString());
            }
        }

        @Test
        @DisplayName("FCM 토큰을 찾을 수 없을 경우 CustomException(FCM_TOKEN_NOT_FOUND)을 던진다")
        void it_throws_exception_when_fcm_token_not_found() {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmTokenValue = "not-found-token";
            Member member = createMember(memberId);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.empty());

                // when & then
                CustomException exception = assertThrows(CustomException.class, () ->
                        notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, List.of())
                );
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FCM_TOKEN_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("FCM 토큰이 다른 사용자의 소유일 경우 CustomException(AUTHORIZATION_DENIED)을 던진다")
        void it_throws_exception_when_fcm_token_belongs_to_another_user() {
            // given
            UUID currentUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            String fcmTokenValue = "other-user-token";

            Member currentUser = createMember(currentUserId);
            Member otherUser = createMember(otherUserId);
            FcmToken tokenOfOtherUser = createFcmToken(otherUser, fcmTokenValue);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
                given(memberRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
                given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.of(tokenOfOtherUser));

                // when & then
                CustomException exception = assertThrows(CustomException.class, () ->
                        notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, List.of())
                );
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHORIZATION_DENIED);
            }
        }

        @Test
        @DisplayName("요청에 DB에 없는 알림 타입이 포함된 경우 CustomException(NOTIFICATION_TYPE_NOT_FOUND)을 던진다")
        void it_throws_exception_when_setting_not_found_in_db() {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmTokenValue = "test-fcm-token";
            Member member = createMember(memberId);
            FcmToken fcmToken = createFcmToken(member, fcmTokenValue);
            NotificationType chatType = createNotificationType("CHAT_MESSAGE");
            DeviceNotificationSetting chatSetting = DeviceNotificationSetting.of(fcmToken, chatType);
            fcmToken.getDeviceNotificationSettingList().add(chatSetting);

            List<UpdateDeviceNotificationSettingRequest> requestList = List.of(
                    new UpdateDeviceNotificationSettingRequest("WORKOUT_REMINDER", true)
            );

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValue(fcmTokenValue)).willReturn(Optional.of(fcmToken));
                given(deviceNotificationSettingRepository.findAllByFcmToken(fcmToken)).willReturn(fcmToken.getDeviceNotificationSettingList());

                // when & then
                CustomException exception = assertThrows(CustomException.class, () ->
                        notificationSettingCommandService.updateDeviceNotificationSetting(fcmTokenValue, requestList)
                );
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND);
            }
        }
    }
}