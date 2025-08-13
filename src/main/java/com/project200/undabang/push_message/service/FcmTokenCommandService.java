package com.project200.undabang.push_message.service;

import com.project200.undabang.member.entity.Member;

public interface FcmTokenCommandService {

    void saveFcmToken(Member member, String fcmToken, String userAgent);

    void deactivateFcmToken(Member member, String fcmToken);
}
