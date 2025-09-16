package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.request.UpdateMemberProfileRequest;
import com.project200.undabang.member.dto.response.UpdateMemberProfileResponse;
import com.project200.undabang.member.service.MemberCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberCommandController {

    private final MemberCommandService memberCommandService;

    @PutMapping("/v1/profile")
    public ResponseEntity<CommonResponse<UpdateMemberProfileResponse>> updateMemberProfile(@RequestBody @Valid UpdateMemberProfileRequest request) {

        return ResponseEntity.ok(CommonResponse.update(memberCommandService.updateMemberProfile(request)));
    }
}
