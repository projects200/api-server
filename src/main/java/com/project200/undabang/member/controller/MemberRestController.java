package com.project200.undabang.member.controller;

import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.service.impl.MemberServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberRestController {
    private final MemberServiceImpl memberService;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<SignUpResponseDto> signUpMember(@RequestHeader("X-USER-ID") String userId,
                                                          @RequestHeader("X-USER-EMAIL") String userEmail,
                                                          @RequestBody @Valid SignUpRequestDto signUpRequestDto){

        signUpRequestDto.setUserId(userId);
        signUpRequestDto.setUserEmail(userEmail);
        SignUpResponseDto responseDto = memberService.completeMemberProfile(signUpRequestDto);

        return ResponseEntity.ok().body(responseDto);
    }

}
