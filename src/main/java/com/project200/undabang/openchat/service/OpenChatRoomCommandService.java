package com.project200.undabang.openchat.service;

import com.project200.undabang.openchat.dto.request.CreateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.response.CreateOpenChatRoomResponse;

public interface OpenChatRoomCommandService {
    CreateOpenChatRoomResponse createOpenChatRoom(CreateOpenChatRoomRequest request);
}
