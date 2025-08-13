package com.project200.undabang.push_message.repository;

import com.project200.undabang.push_message.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByFcmTokenValueAndMember_MemberId(@NonNull String fcmTokenValue, @NonNull UUID memberId);
}
