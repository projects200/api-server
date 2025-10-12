package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.response.ChatMessageDto;
import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.chat.service.ChatQueryService;
import com.project200.undabang.chat.service.ChatUpdateService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryServiceImpl implements ChatQueryService {

    private final MemberRepository memberRepository;
    private final ChatroomMemberRepository chatroomMemberRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatUpdateService chatUpdateService;

    /**
     * 사용자의 채팅방 목록을 반환합니다.
     */
    @Override
    public List<GetMemberChatroomResponse> getMemberChatroomList() {
        Member member = getMember(UserContextHolder.getUserId());

        return chatroomMemberRepository.getChatroomListByMemberId(member);
    }

    /**
     * 주어진 채팅방 ID에 속하는 회원의 채팅 기록을 요청된 조건에 따라 반환합니다.
     */
    @Override
    public GetMemberChatResponse getMemberChat(Long chatroomId, Long prevChatId, Pageable pageable) {
        Member member = getMember(UserContextHolder.getUserId());

        if (!chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)) {
            throw new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND);
        }

        Slice<ChatMessageDto> dtoList = chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member);

        if (prevChatId == null && !dtoList.isEmpty()) {
            Long lastChatId = dtoList.getContent().getLast().getChatId();

            try {
                chatUpdateService.updateLastReadChatId(chatroomId, member, lastChatId);
            } catch (Exception e) {
                log.error("최근에 읽은 메시지 목록 업데이트 실패");
            }
        }

        boolean isOpponentActive = chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)
                .filter(status -> status == ChatroomMemberStatus.ACTIVE)
                .isPresent();

        return GetMemberChatResponse.from(dtoList, isOpponentActive);
    }

    /**
     * 주어진 회원 ID에 해당하는 회원 정보를 반환합니다.
     * 회원 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

}
