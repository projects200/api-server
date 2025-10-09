package com.project200.undabang.chat.repository;

import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.member.entity.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface ChatroomRepositoryCustom {
    Optional<Chatroom> findChatroomBetweenMembers(Member currentMember, Member targetMember);
    Slice<GetMemberChatResponse> getMemberChat(Long chatroomId, Long prevChatId, Pageable pageable, Member member);
}
