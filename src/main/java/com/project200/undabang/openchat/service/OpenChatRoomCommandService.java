package com.project200.undabang.openchat.service;

import com.project200.undabang.openchat.dto.request.CreateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.request.UpdateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.response.CreateOpenChatRoomResponse;
import com.project200.undabang.openchat.dto.response.UpdateOpenChatRoomResponse;

public interface OpenChatRoomCommandService {
    CreateOpenChatRoomResponse createOpenChatRoom(CreateOpenChatRoomRequest request);

    UpdateOpenChatRoomResponse updateOpenChatRoom(Long openChatRoomId, UpdateOpenChatRoomRequest request);
}
