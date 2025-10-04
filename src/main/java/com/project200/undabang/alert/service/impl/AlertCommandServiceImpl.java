package com.project200.undabang.alert.service.impl;

import com.project200.undabang.alert.dto.response.UpdateExerciseEncouragementResponse;
import com.project200.undabang.alert.service.AlertCommandService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCommandServiceImpl implements AlertCommandService {
    private final FcmTokenCommandService fcmTokenCommandService;
    private final MemberRepository memberRepository;

    /**
     * 주어진 FCM 토큰을 활성화합니다.
     * 현재 사용자와 연관된 모든 토큰을 활성화 처리합니다.
     */
    @Override
    public UpdateExerciseEncouragementResponse activateAllExerciseEncouragementToken() {
        Member member = getMember(UserContextHolder.getUserId());

        return UpdateExerciseEncouragementResponse.of(fcmTokenCommandService.activateAllTokens(member));
    }

    /**
     * 주어진 FCM 토큰을 비활성화합니다.
     * 현재 사용자와 연관된 모든 토큰을 비활성화 처리합니다.
     */
    @Override
    public UpdateExerciseEncouragementResponse deactivateAllExerciseEncouragementToken() {
        Member member = getMember(UserContextHolder.getUserId());

        return UpdateExerciseEncouragementResponse.of(fcmTokenCommandService.deactivateAllTokens(member));
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회합니다.
     * 회원 정보를 찾을 수 없는 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
