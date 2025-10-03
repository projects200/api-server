package com.project200.undabang.alert.service.impl;

import com.project200.undabang.alert.service.AlertService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {
    private final FcmTokenCommandService fcmTokenCommandService;
    private final MemberRepository memberRepository;

    /**
     * 사용자 알림을 활성화합니다. 주어진 FCM 토큰을 저장하고 관련된 사용자 정보를 업데이트합니다.
     */
    @Override
    public void activateAlert(String fcmToken) {
        Member member = getMember(UserContextHolder.getUserId());

        if (Objects.nonNull(fcmToken) && !fcmToken.isBlank()) {
            fcmTokenCommandService.activateFcmToken(member, fcmToken);
        }
    }

    /**
     * 주어진 FCM 토큰을 비활성화합니다.
     */
    @Override
    public void deactivateAlert(String fcmToken) {
        Member member = getMember(UserContextHolder.getUserId());

        if (Objects.nonNull(fcmToken) && !fcmToken.isBlank()) {
            fcmTokenCommandService.deactivateFcmToken(member, fcmToken);
        }
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회합니다.
     * 회원 정보를 찾을 수 없는 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
