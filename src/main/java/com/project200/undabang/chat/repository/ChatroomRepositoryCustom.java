package com.project200.undabang.chat.repository;

import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.member.entity.Member;

import java.util.Optional;

public interface ChatroomRepositoryCustom {
    Optional<Chatroom> findChatroomInfoBetweenMembers(Member currentMember, Member targetMember);
}
