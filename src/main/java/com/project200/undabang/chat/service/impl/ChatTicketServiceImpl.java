package com.project200.undabang.chat.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.project200.undabang.chat.dto.response.TicketResponse;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.entity.TicketInfoRecord;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.service.ChatTicketService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChatTicketServiceImpl implements ChatTicketService {

    private final Cache<UUID, TicketInfoRecord> chatTicketCache;
    private final MemberRepository memberRepository;
    private final ChatroomMemberRepository chatroomMemberRepository;

    public ChatTicketServiceImpl(@Qualifier("chatTicketCache") Cache<UUID, TicketInfoRecord> chatTicketCache, MemberRepository memberRepository, ChatroomMemberRepository chatroomMemberRepository) {
        this.chatTicketCache = chatTicketCache;
        this.memberRepository = memberRepository;
        this.chatroomMemberRepository = chatroomMemberRepository;
    }

    /**
     * 주어진 채팅방 ID에 대해 티켓을 발급합니다.
     *
     * @param roomId 티켓을 발급할 채팅방의 ID
     * @return 발급된 티켓 정보를 포함한 TicketResponse 객체
     * @throws CustomException 채팅방이 존재하지 않거나 채팅방에 활성 상태로 참여하지 않은 경우
     */
    @Override
    @Transactional(readOnly = true)
    public TicketResponse issueTicket(Long roomId) {
        Member member = getMember(UserContextHolder.getUserId());

        // 회원이 입력한 채팅방이 본인의 것이 아니거나, 채팅방을 나간 경우
        if (!chatroomMemberRepository.existsByChatroom_IdAndMemberAndChatroomMemberStatus(roomId, member, ChatroomMemberStatus.ACTIVE)) {
            throw new CustomException(ErrorCode.CHATROOM_NOT_FOUND);
        }

        UUID ticketId = UUID.randomUUID();
        TicketInfoRecord record = new TicketInfoRecord(member.getMemberId(), roomId);

        chatTicketCache.put(ticketId, record);

        return TicketResponse.of(ticketId);
    }

    /**
     * 주어진 티켓 ID에 대해 티켓의 유효성을 검증합니다.
     * 검증에 성공하면 티켓 정보를 반환하고, 캐시에서 해당 티켓을 제거합니다.
     * 티켓이 유효하지 않은 경우 null을 반환합니다.
     *
     * @param ticketId 검증할 티켓의 UUID
     * @return 유효한 경우 티켓 정보(TicketInfoRecord 객체), 유효하지 않은 경우 null
     */
    @Override
    @Transactional(readOnly = true)
    public TicketInfoRecord validateTicket(UUID ticketId) {
        TicketInfoRecord record = chatTicketCache.getIfPresent(ticketId);

        // 입력받은 티켓값이 있으면 캐시에서 제거, 없으면 Null 반환
        if (record != null) {
            chatTicketCache.invalidate(ticketId);
        }

        return record;
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
