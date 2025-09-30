package com.project200.undabang.openchat.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.openchat.dto.response.GetOtherMemberOpenChatUrlResponse;
import com.project200.undabang.openchat.service.OpenChatQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OpenChatQueryController {

    private final OpenChatQueryService openChatQueryService;

    @GetMapping("/v1/members/{memberId}/open-chat")
    public ResponseEntity<CommonResponse<GetOtherMemberOpenChatUrlResponse>> getOtherMemberOpenChatUrl(@PathVariable UUID memberId) {

        return ResponseEntity.ok(CommonResponse.success(openChatQueryService.getOtherMemberOpenChatUrl(memberId)));
    }
}
