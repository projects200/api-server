package com.project200.undabang.chat.service;

import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ChatQueryService {
    List<GetMemberChatroomResponse> getMemberChatroomList();

    Slice<GetMemberChatResponse> getMemberChat(Long chatroomId, Long prevChatId, Pageable pageable);
}
