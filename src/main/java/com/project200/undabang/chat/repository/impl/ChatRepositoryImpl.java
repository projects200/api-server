package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.entity.QChat;
import com.project200.undabang.chat.repository.ChatRepositoryCustom;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.notification.fcm.dto.ChatNotificationContent;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRepositoryImpl implements ChatRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * 주어진 채팅 ID를 기반으로 알림에 필요한 채팅 내용을 조회합니다.
     */
    @Override
    public Optional<ChatNotificationContent> findChatContentForNotification(Long chatId) {
        QMember member = QMember.member;
        QChat chat = QChat.chat;

        return Optional.ofNullable(
                queryFactory.select(Projections.constructor(ChatNotificationContent.class,
                                member.memberId,
                                member.memberNickname,
                                chat.chatroom.id,
                                chat.chatContent
                        ))
                        .from(chat)
                        .join(chat.sender, member)
                        .where(chat.id.eq(chatId))
                        .fetchOne()
        );
    }
}
