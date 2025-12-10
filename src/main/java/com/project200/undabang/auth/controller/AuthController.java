package com.project200.undabang.auth.controller;

import com.project200.undabang.auth.dto.request.LoginRequestDto;
import com.project200.undabang.auth.service.AuthService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.common.web.response.SuccessDetails;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    private final FcmTokenCommandService fcmTokenCommandService;

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/login")
    public CommonResponse<Void> loginMember(@RequestHeader(value = "User-Agent", required = false) String userAgent,
                                            @RequestHeader(value = "X-Fcm-Token", required = false) String fcmToken,
                                            @Valid @RequestBody(required = false) LoginRequestDto requestDto) {

        Member member = authService.login();

        if (Objects.nonNull(fcmToken) && !fcmToken.isBlank()) { // fcm 토큰이 있는경우
            if (requestDto == null || requestDto.getPlatform() == null || requestDto.getAccessMode() == null) { // 기기 정보가 없으면 에러 반환
                throw new CustomException(ErrorCode.FCM_DEVICE_INFO_REQUIRED);
            }

            fcmTokenCommandService.saveFcmToken(member, fcmToken, userAgent, requestDto);
        }

        return CommonResponse.success(new SuccessDetails("LOGIN_SUCCESS", "로그인 성공"));
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/logout")
    public CommonResponse<Void> logoutMember(@RequestHeader(value = "X-Fcm-Token", required = false) String fcmToken) {

        Member member = authService.logout();

        if (Objects.nonNull(fcmToken) && !fcmToken.isBlank()) {
            fcmTokenCommandService.deactivateFcmToken(member, fcmToken);
        }

        return CommonResponse.success(new SuccessDetails("LOGOUT_SUCCESS", "로그아웃 성공"));
    }
}
