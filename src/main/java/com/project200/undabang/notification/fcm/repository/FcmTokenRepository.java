package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.notification.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long>, FcmTokenRepositoryCustom {

    Optional<FcmToken> findByFcmTokenValueAndMember_MemberId(@NonNull String fcmTokenValue, @NonNull UUID memberId);

    /**
     * 주어진 토큰 값 목록에 해당하는 FCM 토큰들을 DB에서 삭제(soft delete)합니다.
     *
     * @param fcmTokenValues 삭제할 FCM 토큰 값 리스트
     */
    long deleteByFcmTokenValueIn(@NonNull Collection<String> fcmTokenValues);

}
