package com.project200.undabang.chat.controller;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.service.ChatQueryService;
import com.project200.undabang.common.web.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatQueryController {

    private final ChatQueryService chatQueryService;

    @GetMapping("/v1/chat-rooms")
    public ResponseEntity<CommonResponse<List<GetMemberChatroomResponse>>> getMemberChatroomList() {

        return ResponseEntity.ok(CommonResponse.success(chatQueryService.getMemberChatroomList()));
    }
}
