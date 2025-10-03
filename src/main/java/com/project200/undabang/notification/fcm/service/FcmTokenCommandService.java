package com.project200.undabang.notification.fcm.service;

import com.project200.undabang.member.entity.Member;

import java.util.List;

public interface FcmTokenCommandService {

    void saveFcmToken(Member member, String fcmToken, String userAgent);

    void activateFcmToken(Member member, String fcmToken);

    void deactivateFcmToken(Member member, String fcmToken);

    /**
     * 무효한 FCM 토큰을 DB에서 삭제합니다.
     * 이 메소드는 호출될 때마다 새로운 트랜잭션 내에서 실행됩니다.
     *
     * @param tokensToDelete 삭제할 토큰 값 리스트
     */
    void deleteInvalidTokens(List<String> tokensToDelete);
}
