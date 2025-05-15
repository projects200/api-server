package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthRestController {
    private final MemberService memberService;

    @PostMapping("/auth/sign-up/v1")
    public CommonResponse<SignUpResponseDto> signUpMember(@Valid @RequestBody SignUpRequestDto signUpRequestDto){
        SignUpResponseDto responseDto = memberService.memberSignUp(signUpRequestDto);
        return CommonResponse.success(responseDto);
    }

}
