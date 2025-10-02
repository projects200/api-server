package com.project200.undabang.openchat.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.openchat.dto.response.GetOpenChatUrlResponse;
import com.project200.undabang.openchat.dto.response.GetOtherMemberOpenChatUrlResponse;
import com.project200.undabang.openchat.service.OpenChatRoomQueryService;
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
public class OpenChatRoomQueryController {

    private final OpenChatRoomQueryService openChatQueryService;

    @GetMapping("/v1/members/{memberId}/open-chat")
    public ResponseEntity<CommonResponse<GetOtherMemberOpenChatUrlResponse>> getOtherMemberOpenChatroomUrl(@PathVariable UUID memberId) {

        return ResponseEntity.ok(CommonResponse.success(openChatQueryService.getOtherMemberOpenChatroomUrl(memberId)));
    }

    @GetMapping("/v1/open-chats")
    public ResponseEntity<CommonResponse<GetOpenChatUrlResponse>> getOpenChatroomUrl() {

        return ResponseEntity.ok(CommonResponse.success(openChatQueryService.getOpenChatroomUrl()));
    }
}
