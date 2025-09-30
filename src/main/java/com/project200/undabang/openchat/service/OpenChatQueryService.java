package com.project200.undabang.openchat.service;

import com.project200.undabang.openchat.dto.response.GetOpenChatUrlResponse;
import com.project200.undabang.openchat.dto.response.GetOtherMemberOpenChatUrlResponse;

import java.util.UUID;

public interface OpenChatQueryService {
    GetOtherMemberOpenChatUrlResponse getOtherMemberOpenChatroomUrl(UUID memberId);

    GetOpenChatUrlResponse getOpenChatroomUrl();
}
