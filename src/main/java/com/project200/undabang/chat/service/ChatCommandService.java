package com.project200.undabang.chat.service;

import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;

public interface ChatCommandService {
    CreateChatroomResponse createChatroom(CreateChatroomRequest request);
}
