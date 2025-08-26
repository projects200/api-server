package com.project200.undabang.notification.fcm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FcmTokenRepositoryCustom {

    /**
     * 현재 비활성 상태로 인해 점수가 감소되고 있는 회원들의 FCM 토큰 목록을 페이징하여 조회합니다.
     *
     * @param penaltyThresholdDays 비활성으로 간주하는 마지막 활동 후 경과 일수 (정책 데이터)
     * @param pageable             페이징 정보 (페이지 번호, 페이지 크기)
     * @return 페이징된 FCM 토큰 문자열 목록
     */
    Page<String> findFcmTokensForInactiveMembers(int penaltyThresholdDays, Pageable pageable);
}
