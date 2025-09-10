package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.CheckNicknameDuplicateResponse;
import com.project200.undabang.member.service.MemberQueryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/open")
public class PublicMemberQueryController {
    private final MemberQueryService memberQueryService;


    @GetMapping("/v1/nicknames/check")
    public ResponseEntity<CommonResponse<CheckNicknameDuplicateResponse>> checkDuplicateNickname(
            @RequestParam
            @NotBlank(message = "닉네임을 설정해주세요")
            @Size(min = 1, max = 30, message = "닉네임은 30자 이내로 설정해주세요")
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,30}$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다")
            String nickname) {

        return ResponseEntity.ok(CommonResponse.success(memberQueryService.checkDuplicateNickname(nickname)));
    }
}
