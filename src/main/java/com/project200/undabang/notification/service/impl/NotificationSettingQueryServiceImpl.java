package com.project200.undabang.notification.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.dto.response.GetAllDeviceNotificationSettingsResponse;
import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.repository.DeviceNotificationSettingRepository;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.service.NotificationSettingQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingQueryServiceImpl implements NotificationSettingQueryService {

    private final MemberRepository memberRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final DeviceNotificationSettingRepository deviceNotificationSettingRepository;

    /**
     * 주어진 FCM 토큰에 대한 모든 디바이스 알림 설정을 조회합니다.
     */
    @Override
    public List<GetAllDeviceNotificationSettingsResponse> getAllDeviceNotificationSettings(String fcmToken) {
        Member member = getMember(UserContextHolder.getUserId());
        FcmToken savedFcmToken = getFcmToken(member.getMemberId(), fcmToken);

        List<DeviceNotificationSetting> deviceNotificationSettingList = deviceNotificationSettingRepository.findAllByFcmToken(savedFcmToken);

        return deviceNotificationSettingList.stream().map(GetAllDeviceNotificationSettingsResponse::from).toList();
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회합니다.
     * 회원 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 주어진 회원 ID와 FCM 토큰 값을 기반으로 FCM 토큰 정보를 조회합니다.
     * FCM 토큰 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private FcmToken getFcmToken(UUID memberId, String fcmToken) {
        return fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmToken, memberId).orElseThrow(
                () -> new CustomException(ErrorCode.FCM_TOKEN_NOT_EXIST)
        );
    }
}
