package com.project200.undabang.chat.repository;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface ChatroomMemberRepositoryCustom {
    List<GetMemberChatroomResponse> getChatroomListByMemberId(Member member);
    Optional<ChatroomMemberStatus> getOpponentStatusByChatroomId(Long chatroomId, Member member);
    boolean checkOtherMemberBlocked(Chatroom currentChatroom, Member currentMember);
}
