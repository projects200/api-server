package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.service.MemberQueryService;
import com.project200.undabang.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthRestController {
    private final MemberService memberService;
    private final MemberQueryService memberQueryService;

    @PostMapping("/v1/sign-up")
    public CommonResponse<SignUpResponseDto> signUpMember(@Valid @RequestBody SignUpRequestDto signUpRequestDto){
        SignUpResponseDto responseDto = memberService.memberSignUp(signUpRequestDto);
        return CommonResponse.success(responseDto);
    }

    /**
     * 현재 회원의 등록 상태를 조회합니다.
     *
     * @return 회원 등록 상태를 포함한 ApiResponse 객체를 반환합니다.
     *         반환되는 데이터는 MemberRegistrationStatusResponseDto 형태입니다.
     */
    @GetMapping("/v1/registration-status")
    public ResponseEntity<CommonResponse<MemberRegistrationStatusResponseDto>> getRegistrationStatus() {
        return ResponseEntity.ok(CommonResponse.success(memberQueryService.getRegistrationStatus()));
    }
}
