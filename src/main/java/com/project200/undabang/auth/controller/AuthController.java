package com.project200.undabang.auth.controller;

import com.project200.undabang.auth.service.AuthService;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.common.web.response.SuccessDetails;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
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
                                            @RequestHeader(value = "X-Fcm-Token", required = false) String fcmToken) {

        Member member = authService.login();

        if (Objects.nonNull(fcmToken) && !fcmToken.isBlank()) {
            fcmTokenCommandService.saveFcmToken(member, fcmToken, userAgent);
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
