package com.project200.undabang.chat.service;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;

import java.util.List;

public interface ChatQueryService {
    List<GetMemberChatroomResponse> getMemberChatroomList();
}
