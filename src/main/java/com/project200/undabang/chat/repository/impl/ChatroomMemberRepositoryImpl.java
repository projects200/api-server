package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.entity.*;
import com.project200.undabang.chat.repository.ChatroomMemberRepositoryCustom;
import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberBlock;
import com.project200.undabang.member.entity.QMemberPicture;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatroomMemberRepositoryImpl implements ChatroomMemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 주어진 채팅방과 현재 회원 정보를 기반으로 다른 회원이 차단되었는지 확인합니다.
     */
    @Override
    public boolean checkOtherMemberBlocked(Chatroom currentChatroom, Member currentMember) {
        QChatroomMember chatroomMember = QChatroomMember.chatroomMember;
        QMemberBlock memberBlock = QMemberBlock.memberBlock;

        Integer result = queryFactory
                .selectOne()
                .from(chatroomMember)
                .join(memberBlock).on(
                        (memberBlock.blocker.eq(currentMember) // 내가 차단자인 경우
                                .and(memberBlock.blocked.eq(chatroomMember.member))) // 상대가 차단당한 경우
                                .or(memberBlock.blocker.eq(chatroomMember.member) // 상대가 차단자인 경우
                                        .and(memberBlock.blocked.eq(currentMember)))  // 내가 차단당한 경우
                                .and(memberBlock.memberBlockDeletedAt.isNull()) // 차단 해제하지 않은 경우
                )
                .where(
                        chatroomMember.chatroom.eq(currentChatroom), // 현재 채팅방에서
                        chatroomMember.member.ne(currentMember) // 내가 아닌 사람을 가져옴
                )
                .fetchFirst(); // 1:1 채팅이니까 하나만 가져옴

        return result != null; // 있으면 true 없으면 false
    }

    /**
     * 주어진 채팅방 ID와 현재 회원 정보를 기반으로 상대방의 채팅방 상태를 조회합니다.
     */
    @Override
    public Optional<ChatroomMemberStatus> getOpponentStatusByChatroomId(Long chatroomId, Member currentMember) {
        QChatroomMember cm = QChatroomMember.chatroomMember;

        ChatroomMemberStatus status = queryFactory
                .select(cm.chatroomMemberStatus)
                .from(cm)
                .where(
                        cm.chatroom.id.eq(chatroomId),
                        cm.member.ne(currentMember)
                )
                .fetchOne();

        return Optional.ofNullable(status);
    }

    /**
     * 주어진 회원(Member)의 ID를 기준으로 해당 회원이 속한 채팅방의 목록을 반환합니다.
     */
    @Override
    public List<GetMemberChatroomResponse> getChatroomListByMemberId(Member member) {
        QMember otherMember = QMember.member;
        QMemberPicture otherMemberPicture = QMemberPicture.memberPicture;
        QPicture otherPicture = QPicture.picture;
        QChatroom chatroom = QChatroom.chatroom;

        // 셀프조인같이 한 쿼리 내부에서 동일한 Q타입을 두번 이상 생성해야 할때, QueryDSL이 구분하기 위해서 new 로 객체 생성
        QChatroomMember cm = new QChatroomMember("cm");
        QChatroomMember otherCM = new QChatroomMember("otherCM");

        return queryFactory
                .select(Projections.constructor(GetMemberChatroomResponse.class,
                        otherMember.memberId,
                        chatroom.id,
                        otherMember.memberNickname,
                        otherMemberPicture.memberPicturesUrl,
                        otherPicture.pictureUrl,
                        chatroom.lastChatContent,
                        chatroom.lastChatReceivedAt,
                        createUnreadCountSubQuery(cm, chatroom, member)
                ))
                .from(cm)
                .join(cm.chatroom, chatroom) // 내 참여 정보를 통해 채팅방에 연결
                .join(chatroom.chatroomMembers, otherCM) // 채팅방에서 다른 참여자들과 연결
                .join(otherCM.member, otherMember) // 다른 참여자정보로 다른 회원과 연결
                .leftJoin(otherMember.memberPicture, otherMemberPicture) // 다른 회원의 썸네일 사진 가져오기
                .leftJoin(otherMemberPicture.picture, otherPicture) // 다른 회원의 프로필 사진 가져오기
                .where(
                        cm.member.eq(member),
                        cm.chatroomMemberStatus.eq(ChatroomMemberStatus.ACTIVE), // 활성화된 채팅방만 가져오기 (내가 나간 채팅방은 조회 안함)
                        otherCM.member.ne(member) // 상대방이 내가 되면 안됨
                )
                .orderBy(chatroom.lastChatReceivedAt.desc())
                .fetch();
    }

    /**
     * 읽지 않은 메시지의 개수를 계산하기 위한 서브쿼리를 생성합니다.
     */
    private JPQLQuery<Long> createUnreadCountSubQuery(QChatroomMember cm, QChatroom chatroom, Member currentMember) {
        QChat chat = QChat.chat;

        return queryFactory
                .select(chat.id.count())
                .from(chat)
                .where(
                        chat.chatroom.eq(chatroom), // 서브쿼리 채팅방이 메인 채팅방과 같아야 함
                        chat.id.gt(cm.lastReadChatId.coalesce(0L)), // 채팅방의 메시지 중 내가 마지막으로 읽은것보다 커야 하며, lastReadChatId가 null일 경우 0으로 처리
                        chat.chatType.eq(ChatType.USER), // 사용자가 쓴 채팅만 읽기
                        chat.sender.ne(currentMember) // 내가 작성한 채팅은 무시
                );
    }
}
