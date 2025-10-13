package com.project200.undabang.chat.service;

import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.dto.response.CreateMessageResponse;

public interface ChatCommandService {
    CreateChatroomResponse createChatroom(CreateChatroomRequest request);
    CreateMessageResponse createMessage(Long chatroomId, CreateMessageRequest request);

    void leaveChatroom(Long chatroomId);
}
