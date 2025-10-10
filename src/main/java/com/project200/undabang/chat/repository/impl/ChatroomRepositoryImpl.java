package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.dto.response.ChatMessageDto;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.QChat;
import com.project200.undabang.chat.entity.QChatroom;
import com.project200.undabang.chat.entity.QChatroomMember;
import com.project200.undabang.chat.repository.ChatroomRepositoryCustom;
import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberPicture;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.project200.undabang.chat.entity.QChat.chat;

@Repository
@RequiredArgsConstructor
public class ChatroomRepositoryImpl implements ChatroomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 주어진 채팅방 ID와 이전 채팅 ID, 페이지 요청 정보에 따라 해당 채팅방에서의 채팅 내용을 조회합니다.
     * Pageable 방식의 페이지네이션이 아니라 Slice를 사용해서 커서방식 (no-offset) 방식의 페이지네이션을 조회합니다.
     * 왜냐하면 무한 스크롤 방식의 구현이 필요하기 때문에 기존 방식으로는 성능 저하가 유발될 수 있기 때문입니다.
     */
    @Override
    public Slice<ChatMessageDto> getMemberChat(Long chatroomId, Long prevChatId, Pageable pageable, Member currentMember) {
        QChat chat = QChat.chat;
        QMemberPicture memberPicture = QMemberPicture.memberPicture;
        QPicture picture = QPicture.picture;
        QMember member = QMember.member;

        List<ChatMessageDto> result = queryFactory
                .select(Projections.constructor(
                        ChatMessageDto.class,
                        chat.id,
                        member.memberId,
                        member.memberNickname,
                        picture.pictureUrl,
                        memberPicture.memberPicturesUrl,
                        chat.chatContent,
                        chat.chatType,
                        chat.chatCreatedAt,
                        isMyChat(currentMember) // 동적 쿼리 (내 채팅과 타인의 채팅 구분) 구문을 생성하는 헬퍼 메소드
                ))
                .from(chat)
                .join(chat.sender, member)
                .leftJoin(member.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .where(
                        chat.chatroom.id.eq(chatroomId), // 내가 속한 채팅방
                        olderThanPrevChatId(prevChatId) // 커서기반 페이지네이션을 위한 동적 조건
                )
                .orderBy(chat.id.desc()) // 최신 메시지부터 읽어줌
                .limit(pageable.getPageSize() + 1) // 다음 채팅이 존재하는지 아닌지 구별할 수 있도록 30+1 로 설정함
                .fetch();

        return checkNextPage(pageable, result);
    }

    /**
     * 주어진 Pageable 객체와 결과 리스트를 기반으로 Slice 객체를 생성하고,
     * 페이지에 포함될 요소와 다음 페이지 존재 여부를 판단합니다.
     */
    private Slice<ChatMessageDto> checkNextPage(Pageable pageable, List<ChatMessageDto> result) {

        boolean hasNext = false;

        if (result.size() > pageable.getPageSize()) { // 다음 채팅이 존재하는 경우
            hasNext = true;
            result.remove(pageable.getPageSize()); // 31번째 데이터는 삭제 (다음것이 있음을 확인했으므로)
        }

        Collections.reverse(result); // 클라이언트에서 사용하기 편하게 오름차순으로 정리

        return new SliceImpl<>(result, pageable, hasNext);
    }

    /**
     * 주어진 이전 채팅 ID(prevChatId)보다 더 오래된 채팅을 필터링하는 조건을 생성합니다.
     * prevChatId == null 인 경우는 첫 페이지 조회이므로 조건이 적용되지 않도록 null 반환
     */
    private BooleanExpression olderThenPrevChatId(Long prevChatId) {
        if (prevChatId == null) {
            return null;
        }

        return chat.id.lt(prevChatId);
    }

    /**
     * 주어진 사용자가 현재 채팅의 발신자인지를 확인하는 조건을 생성합니다.
     */
    private BooleanExpression isMyChat(Member currentUser) {
        return new CaseBuilder()
                .when(chat.sender.eq(currentUser)).then(true)
                .otherwise(false);
    }

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
