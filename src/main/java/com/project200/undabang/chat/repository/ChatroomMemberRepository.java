package com.project200.undabang.chat.repository;

import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatroomMemberRepository extends JpaRepository<ChatroomMember, Long>, ChatroomMemberRepositoryCustom {
    Optional<ChatroomMember> findByChatroomAndMember(Chatroom chatroom, Member member);
}
