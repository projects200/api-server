package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.entity.QChatroomMember;
import com.project200.undabang.chat.repository.ChatroomMemberRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatroomMemberRepositoryCustomImpl implements ChatroomMemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 지정된 채팅방(chatroomId)에 속한 모든 회원이 활성 상태인지 확인합니다.
     * 만약 모든 회원의 수가 2면 T, 그 외는 F를 반환합니다.
     * 반환값이 없는 경우도 체크하였습니다.
     */
    @Override
    public boolean checkAllMembersActive(Long chatroomId) {
        QChatroomMember chatroomMember = QChatroomMember.chatroomMember;

        Long result = queryFactory
                .select(chatroomMember.count())
                .from(chatroomMember)
                .where(
                        chatroomMember.chatroom.id.eq(chatroomId),
                        chatroomMember.chatroomMemberStatus.eq(ChatroomMemberStatus.ACTIVE)
                )
                .fetchOne();

        return result != null && result == 2;
    }
}
