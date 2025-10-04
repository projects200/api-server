package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.FcmToken;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FcmTokenCommandServiceImpl implements FcmTokenCommandService {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * 주어진 회원에 대한 FCM 토큰을 저장하거나 활성화합니다.
     * 데이터베이스에 동일한 FCM 토큰이 존재하면 해당 토큰을 활성화하고,
     * 존재하지 않을 경우 새로 생성하여 저장합니다.
     */
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


    /**
     * 주어진 회원의 특정 FCM 토큰을 비활성화합니다.
     * 해당 토큰이 존재하지 않을 경우 경고 메시지를 기록합니다.
     */
    @Override
    public void deactivateFcmToken(Member member, String fcmToken) {
        fcmTokenRepository.findByFcmTokenValueAndMember_MemberId(fcmToken, member.getMemberId())
                .ifPresentOrElse(existingToken -> {
                    log.debug("FCM 토큰을 비활성화합니다. 회원 id: {}", member.getMemberId());
                    existingToken.deactivate();
                }, () -> log.warn("비활성화할 FCM 토큰이 존재하지 않습니다. 회원 id: {}", member.getMemberId()));
    }

    /**
     * 특정 회원의 모든 비활성화된 FCM 토큰을 활성화합니다.
     */
    @Override
    public Long activateAllTokens(Member member) {
        return fcmTokenRepository.activateAllInactiveTokensByMember(member);
    }

    /**
     * 지정된 회원의 모든 활성화된 FCM 토큰을 비활성화합니다.
     */
    @Override
    public Long deactivateAllTokens(Member member) {
        return fcmTokenRepository.deactivateAllActiveTokensByMember(member);
    }

    /**
     * 무효한 FCM 토큰을 DB에서 삭제합니다.
     * 이 메소드는 호출될 때마다 새로운 트랜잭션 내에서 실행됩니다.
     *
     * @param tokensToDelete 삭제할 토큰 값 리스트
     */
    @Override
    public void deleteInvalidTokens(List<String> tokensToDelete) {
        if (tokensToDelete == null || tokensToDelete.isEmpty()) {
            return;
        }
        try {
            long deletedCount = fcmTokenRepository.deleteByFcmTokenValueIn(tokensToDelete);
            log.info("[토큰 정리] 총 {}개의 무효 토큰이 성공적으로 삭제되었습니다.", deletedCount);
        } catch (Exception e) {
            log.error("[토큰 정리 실패] 무효 토큰 삭제 중 DB 오류 발생", e);
        }
    }
}
