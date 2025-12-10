package com.project200.undabang.notification.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.dto.response.GetAllDeviceNotificationSettingsResponse;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class NotificationSettingQueryServiceImplTest {

    @InjectMocks
    private NotificationSettingQueryServiceImpl notificationSettingQueryService;

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
        return FcmToken.builder().member(member).fcmTokenValue(tokenValue).build();
    }

    private NotificationType createNotificationType(String code) {
        return NotificationType.builder().notificationTypeCode(code).build();
    }

    private DeviceNotificationSetting createDeviceSetting(FcmToken token, NotificationType type, boolean isEnabled) {
        return DeviceNotificationSetting.builder()
                .fcmToken(token)
                .notificationType(type)
                .isEnabled(isEnabled)
                .build();
    }

    @Nested
    @DisplayName("디바이스 알림 설정 전체 조회 기능은")
    class Describe_getAllDeviceNotificationSettings {

        @Test
        @DisplayName("성공적으로 모든 알림 설정을 조회하여 DTO 리스트로 반환한다")
        void it_returns_a_list_of_dtos_successfully() {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmTokenValue = "test-fcm-token";

            Member member = createMember(memberId);
            FcmToken fcmToken = createFcmToken(member, fcmTokenValue);

            NotificationType chatType = createNotificationType("CHAT_MESSAGE");
            NotificationType workoutType = createNotificationType("WORKOUT_REMINDER");

            // DTO의 from() 메소드가 호출할 수 있도록 실제 객체를 생성하여 리스트를 구성합니다.
            DeviceNotificationSetting setting1 = createDeviceSetting(fcmToken, chatType, true);
            DeviceNotificationSetting setting2 = createDeviceSetting(fcmToken, workoutType, false);
            List<DeviceNotificationSetting> settingList = List.of(setting1, setting2);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, memberId)).willReturn(Optional.of(fcmToken));
                given(deviceNotificationSettingRepository.findAllByFcmToken(fcmToken)).willReturn(settingList);

                // when: 이 내부에서 GetAllDeviceNotificationSettingsResponse.from()이 호출됩니다.
                List<GetAllDeviceNotificationSettingsResponse> response = notificationSettingQueryService.getAllDeviceNotificationSettings(fcmTokenValue);

                // then
                assertThat(response).hasSize(2);
                assertThat(response)
                        .extracting(GetAllDeviceNotificationSettingsResponse::getType, GetAllDeviceNotificationSettingsResponse::isEnabled)
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
                        notificationSettingQueryService.getAllDeviceNotificationSettings("any-token")
                );
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
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
                given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, memberId)).willReturn(Optional.empty());

                // when & then
                CustomException exception = assertThrows(CustomException.class, () ->
                        notificationSettingQueryService.getAllDeviceNotificationSettings(fcmTokenValue)
                );
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FCM_TOKEN_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("알림 설정이 하나도 없는 경우 빈 리스트를 반환한다")
        void it_returns_an_empty_list_when_no_settings_exist() {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmTokenValue = "token-with-no-settings";
            Member member = createMember(memberId);
            FcmToken fcmToken = createFcmToken(member, fcmTokenValue);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, memberId)).willReturn(Optional.of(fcmToken));
                given(deviceNotificationSettingRepository.findAllByFcmToken(fcmToken)).willReturn(Collections.emptyList());

                // when
                List<GetAllDeviceNotificationSettingsResponse> response = notificationSettingQueryService.getAllDeviceNotificationSettings(fcmTokenValue);

                // then
                assertThat(response).isNotNull().isEmpty();
            }
        }
    }
}