package com.project200.undabang.notification.fcm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 알림 발송에 필요한 데이터를 담는 불변 DTO 입니다.
 *
 * @param targetUserToken 알림을 받을 사용자의 FCM 토큰
 * @param title           알림의 제목
 * @param body            알림의 본문
 * @param imageUrl        알림의 이미지
 */
public record NotificationPayload(
        @NotBlank(message = "FCM 토큰은 비어있을 수 없습니다.")
        String targetUserToken,

        String title,

        @NotBlank(message = "알림 내용은 비어있을 수 없습니다.")
        String body,

        String imageUrl
) {
}
