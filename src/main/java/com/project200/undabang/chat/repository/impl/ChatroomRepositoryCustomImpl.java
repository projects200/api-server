package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.QChatroom;
import com.project200.undabang.chat.entity.QChatroomMember;
import com.project200.undabang.chat.repository.ChatroomRepositoryCustom;
import com.project200.undabang.member.entity.Member;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatroomRepositoryCustomImpl implements ChatroomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 두 명의 회원(currentMember와 targetMember) 사이에 존재하는 채팅방을 검색합니다.
     */
    @Override
    public Optional<Chatroom> findChatroomBetweenMembers(Member currentMember, Member targetMember) {
        QChatroom chatroom = QChatroom.chatroom;
        Long chatroomId = findChatroomIdBetweenMembers(currentMember, targetMember);

        if (chatroomId == null) {
            return Optional.empty();
        }

        Chatroom result = queryFactory.selectFrom(chatroom)
                .where(chatroom.id.eq(chatroomId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 정확히 두 명의 회원(currentMember와 targetMember)이 포함된 채팅방의 ID를 찾습니다.
     */
    private Long findChatroomIdBetweenMembers(Member currentMember, Member targetMember) {
        QChatroomMember chatroomMember = QChatroomMember.chatroomMember;

        return queryFactory
                .select(chatroomMember.chatroom.id)
                .from(chatroomMember)
                .where(chatroomMember.member.in(currentMember, targetMember))
                .groupBy(chatroomMember.chatroom.id)
                .having(chatroomMember.count().eq(2L))
                .fetchOne();
    }
}
