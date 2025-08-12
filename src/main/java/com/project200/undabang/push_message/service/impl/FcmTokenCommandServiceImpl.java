package com.project200.undabang.push_message.service.impl;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.push_message.entity.FcmToken;
import com.project200.undabang.push_message.repository.FcmTokenRepository;
import com.project200.undabang.push_message.service.FcmTokenCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FcmTokenCommandServiceImpl implements FcmTokenCommandService {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public void saveFcmToken(Member member, String fcmToken, String userAgent) {
        fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmToken, member.getMemberId())
                .ifPresentOrElse(existingToken -> {
                    log.debug("FCM 토큰이 이미 존재합니다. 회원 id: {}", member.getMemberId());

                    existingToken.activate();
                }, () -> {
                    log.debug("새로운 FCM 토큰을 저장합니다. 회원 id: {}", member.getMemberId());
                    FcmToken newToken = FcmToken.builder()
                            .member(member)
                            .fcmTokenValue(fcmToken)
                            .fcmTokenUserAgent(userAgent)
                            .build();

                    fcmTokenRepository.save(newToken);
                });
    }

    @Override
    public void deactivateFcmToken(Member member, String fcmToken) {
        fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmToken, member.getMemberId())
                .ifPresentOrElse(existingToken -> {
                    log.debug("FCM 토큰을 비활성화합니다. 회원 id: {}", member.getMemberId());
                    existingToken.deactivate();
                }, () -> log.warn("비활성화할 FCM 토큰이 존재하지 않습니다. 회원 id: {}", member.getMemberId()));
    }
}
