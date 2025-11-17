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
import com.project200.undabang.notification.service.NotificationSettingCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationSettingCommandServiceImpl implements NotificationSettingCommandService {

    private final MemberRepository memberRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    /**
     * 디바이스 알림 설정을 업데이트합니다.
     * 주어진 FCM 토큰과 요청 목록을 기반으로 알림 설정을 갱신합니다.
     */
    @Transactional
    @Override
    public UpdateDeviceNotificationSettingResponse updateDeviceNotificationSetting(String fcmToken, List<UpdateDeviceNotificationSettingRequest> requestList) {
        Member member = getMember(UserContextHolder.getUserId());
        FcmToken savedFcmToken = getFcmToken(member, fcmToken);
        List<DeviceNotificationSetting> deviceNotificationSettingList = deviceNotificationSettingRepository.findAllByFcmToken(savedFcmToken);

        Map<NotificationType, DeviceNotificationSetting> deviceNotificationSettingMap = deviceNotificationSettingList.stream()
                .collect(Collectors.toMap(DeviceNotificationSetting::getNotificationType, Function.identity()));

        for (UpdateDeviceNotificationSettingRequest request : requestList) {
            DeviceNotificationSetting setting = deviceNotificationSettingMap.get(request.getType());

            if (setting == null) {
                throw new CustomException(ErrorCode.DEVICE_NOTIFICATION_SETTING_NOT_FOUND);
            }

            setting.updateEnabledStatus(request.getEnabled());
        }

        List<NotificationSettingRecord> recordList = deviceNotificationSettingList.stream()
                .map(setting -> new NotificationSettingRecord(
                        setting.getNotificationType(),
                        setting.getIsEnabled()
                )).toList();

        return UpdateDeviceNotificationSettingResponse.of(fcmToken, recordList);
    }

    /**
     * 주어진 회원 ID를 사용하여 회원 정보를 조회합니다.
     * 만약 회원 정보가 존재하지 않을 경우, MEMBER_NOT_FOUND 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 주어진 FCM 토큰 값에 해당하는 FcmToken 엔티티를 조회합니다.
     * 조회된 FcmToken이 주어진 회원의 FcmToken이 아닌 경우 예외를 발생시킵니다.
     */
    private FcmToken getFcmToken(Member member, String fcmToken) {
        FcmToken savedToken = fcmTokenRepository.findByFcmTokenValue(fcmToken).orElseThrow(() -> new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

        if (!savedToken.getMember().getMemberId().equals(member.getMemberId())) {
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }

        return savedToken;
    }
}
