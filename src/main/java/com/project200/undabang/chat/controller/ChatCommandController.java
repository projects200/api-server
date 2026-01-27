package com.project200.undabang.chat.controller;

import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.dto.response.CreateMessageResponse;
import com.project200.undabang.chat.dto.response.TicketResponse;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.chat.service.ChatTicketService;
import com.project200.undabang.common.web.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatCommandController {

    private final ChatCommandService chatCommandService;
    private final ChatTicketService chatTicketService;

    /**
     * 새로운 채팅방을 생성합니다.
     */
    @PostMapping("/v1/chat-rooms")
    public ResponseEntity<CommonResponse<CreateChatroomResponse>> createChatRoom(@Valid @RequestBody CreateChatroomRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(chatCommandService.createChatroom(request)));
    }

    /**
     * 지정된 채팅방에 새 메시지를 생성합니다.
     */
    @PostMapping("/v1/chat-rooms/{chatroomId}/messages")
    public ResponseEntity<CommonResponse<CreateMessageResponse>> createMessage(@PathVariable Long chatroomId,
                                                                               @Valid @RequestBody CreateMessageRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(chatCommandService.createMessage(chatroomId, request)));
    }

    /**
     * 주어진 채팅방 ID에 기반하여 티켓을 생성합니다.
     *
     * @param chatroomId 티켓을 생성할 대상 채팅방의 ID
     * @return 생성된 티켓 정보가 포함된 HTTP 상태 코드 201(CREATED) 응답
     */
    @PostMapping("/v1/chat-rooms/{chatroomId}/ticket")
    public ResponseEntity<CommonResponse<TicketResponse>> createTicket(@PathVariable Long chatroomId) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(chatTicketService.issueTicket(chatroomId)));
    }

    /**
     * 주어진 채팅방 ID에 해당하는 채팅방을 삭제합니다.
     */
    @DeleteMapping("/v1/chat-rooms/{chatroomId}")
    public ResponseEntity<CommonResponse<Void>> leaveChatRoom(@PathVariable Long chatroomId) {

        chatCommandService.leaveChatroom(chatroomId);
        return ResponseEntity.ok(CommonResponse.delete(null));
    }
}
