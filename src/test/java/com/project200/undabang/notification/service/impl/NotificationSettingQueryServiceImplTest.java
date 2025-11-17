package com.project200.undabang.notification.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.dto.response.GetAllDeviceNotificationSettingsResponse;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class NotificationSettingQueryServiceImplTest {

    private final UUID memberId = UUID.randomUUID();
    private final String fcmTokenValue = "test-fcm-token";

    @InjectMocks
    private NotificationSettingQueryServiceImpl notificationSettingQueryService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    private Member createMember() {
        return Member.builder().memberId(memberId).build();
    }

    private FcmToken createFcmToken(Member member) {
        return FcmToken.from(member, fcmTokenValue, "test-ua");
    }

    @Nested
    @DisplayName("디바이스 알림 설정 전체 조회 테스트")
    class GetAllDeviceNotificationSettingsTest {

        @Test
        @DisplayName("성공적으로 모든 알림 설정을 조회한다")
        void shouldGetAllSettingsSuccessfully() {
            // given
            Member member = createMember();
            FcmToken fcmToken = createFcmToken(member);

            DeviceNotificationSetting setting1 = DeviceNotificationSetting.of(fcmToken, NotificationType.CHAT_MESSAGE);
            DeviceNotificationSetting setting2 = DeviceNotificationSetting.of(fcmToken, NotificationType.WORKOUT_REMINDER);
            List<DeviceNotificationSetting> settingList = List.of(setting1, setting2);

            // UserContextHolder.getUserId()가 static 메소드이므로 mockStatic 사용
            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, memberId)).willReturn(Optional.of(fcmToken));
                given(deviceNotificationSettingRepository.findAllByFcmToken(fcmToken)).willReturn(settingList);

                // when
                List<GetAllDeviceNotificationSettingsResponse> response = notificationSettingQueryService.getAllDeviceNotificationSettings(fcmTokenValue);

                // then
                assertThat(response).hasSize(2);
                assertThat(response)
                        .extracting(GetAllDeviceNotificationSettingsResponse::getType)
                        .containsExactlyInAnyOrder(NotificationType.CHAT_MESSAGE, NotificationType.WORKOUT_REMINDER);
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
                    notificationSettingQueryService.getAllDeviceNotificationSettings(fcmTokenValue);
                });
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("FCM 토큰을 찾을 수 없을 경우 CustomException(FCM_TOKEN_NOT_EXIST)을 던진다")
        void shouldThrowException_whenFcmTokenNotFound() {
            // given
            Member member = createMember();

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, memberId)).willReturn(Optional.empty());

                // when & then
                CustomException exception = assertThrows(CustomException.class, () -> {
                    notificationSettingQueryService.getAllDeviceNotificationSettings(fcmTokenValue);
                });
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FCM_TOKEN_NOT_EXIST);
            }
        }

        @Test
        @DisplayName("알림 설정이 하나도 없는 경우 빈 리스트를 반환한다")
        void shouldReturnEmptyList_whenNoSettingsExist() {
            // given
            Member member = createMember();
            FcmToken fcmToken = createFcmToken(member);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmTokenValue, memberId)).willReturn(Optional.of(fcmToken));
                given(deviceNotificationSettingRepository.findAllByFcmToken(fcmToken)).willReturn(Collections.emptyList());

                // when
                List<GetAllDeviceNotificationSettingsResponse> response = notificationSettingQueryService.getAllDeviceNotificationSettings(fcmTokenValue);

                // then
                assertThat(response).isNotNull();
                assertThat(response).isEmpty();
            }
        }
    }
}