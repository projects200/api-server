package com.project200.undabang.chat.controller;

import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.service.ChatQueryService;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.common.web.response.SliceResponse;
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

    @GetMapping("/v1/chat-rooms/{chatroomId}/messages")
    public ResponseEntity<CommonResponse<SliceResponse<GetMemberChatResponse>>> getMemberChat(@PathVariable Long chatroomId,
                                                                                              @RequestParam(value = "prevChatId", required = false) Long prevChatId,
                                                                                              @PageableDefault(size = 30) Pageable pageable) {

        return ResponseEntity.ok(CommonResponse.success(new SliceResponse(chatQueryService.getMemberChat(chatroomId, prevChatId, pageable))));
    }
}
