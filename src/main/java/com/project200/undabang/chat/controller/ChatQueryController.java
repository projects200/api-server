package com.project200.undabang.chat.controller;

import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.dto.response.GetNewChatResponse;
import com.project200.undabang.chat.service.ChatQueryService;
import com.project200.undabang.common.web.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatQueryController {

    private final ChatQueryService chatQueryService;

    /**
     * 회원이 참여 중인 채팅방 목록을 조회합니다.
     */
    @GetMapping("/v1/chat-rooms")
    public ResponseEntity<CommonResponse<List<GetMemberChatroomResponse>>> getMemberChatroomList() {

        return ResponseEntity.ok(CommonResponse.success(chatQueryService.getMemberChatroomList()));
    }

    /**
     * 지정된 채팅방에서 회원의 채팅 메시지를 조회합니다.
     */
    @GetMapping("/v1/chat-rooms/{chatroomId}/messages")
    public ResponseEntity<CommonResponse<GetMemberChatResponse>> getMemberChat(@PathVariable Long chatroomId,
                                                                               @RequestParam(value = "prevChatId", required = false) Long prevChatId,
                                                                               @PageableDefault(size = 30) Pageable pageable) {

        return ResponseEntity.ok(CommonResponse.success(chatQueryService.getMemberChat(chatroomId, prevChatId, pageable)));
    }

    /**
     * 지정된 채팅방 ID를 기반으로 채팅방에서 새로 도착한 채팅 메시지를 조회합니다.
     */
    @GetMapping("/v1/chat-rooms/{chatroomId}/messages/new")
    public ResponseEntity<CommonResponse<GetNewChatResponse>> getNewChat(@PathVariable Long chatroomId) {

        return ResponseEntity.ok(CommonResponse.success(chatQueryService.getNewChat(chatroomId)));
    }
}
