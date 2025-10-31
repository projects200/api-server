package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.response.ChatMessageDto;
import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.dto.response.GetNewChatResponse;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
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

        validateActiveChatroomMember(member, chatroomId);
        Slice<ChatMessageDto> dtoList = chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member);

        // 첫 페이지 로드시에만 읽음 상태 업데이트
        if (prevChatId == null) {
            updateLastReadStatus(chatroomId, member, dtoList.getContent());
        }

        boolean isOpponentActive = getOpponentStatus(chatroomId, member);
        boolean isOpponentBlocked = getOpponentBlocked(chatroomId, member);

        return GetMemberChatResponse.from(dtoList, isOpponentActive, isOpponentBlocked);
    }

    /**
     * 입력된 채팅방 ID를 기반으로 사용자가 읽지 않은 새 채팅 메시지 목록을 반환하며,
     * 상대방의 현재 활성화 상태를 포함합니다.
     */
    @Override
    public GetNewChatResponse getNewChat(Long chatroomId) {
        Member member = getMember(UserContextHolder.getUserId());
        ChatroomMember chatroomMember = validateActiveChatroomMember(member, chatroomId);
        Long lastChatId = chatroomMember.getLastReadChatId();

        // 혹시 마지막으로 읽은 값이 없는 경우
        if (lastChatId == null) {
            lastChatId = -1L;
        }

        List<ChatMessageDto> dtoList = chatroomRepository.getNewMemberChat(member, chatroomId, lastChatId);

        updateLastReadStatus(chatroomId, member, dtoList);
        boolean isOpponentActive = getOpponentStatus(chatroomId, member);

        return GetNewChatResponse.of(dtoList, isOpponentActive);
    }


    /**
     * 채팅 메시지 목록을 기반으로 사용자의 마지막 읽은 메시지 상태를 업데이트합니다.
     * 채팅 메시지가 비어 있지 않은 경우에만 업데이트를 수행하며, 실패 시 로그를 기록합니다.
     */
    private void updateLastReadStatus(Long chatroomId, Member member, List<ChatMessageDto> dtoList) {
        if (!dtoList.isEmpty()) {
            Long lastChatId = dtoList.getLast().getChatId();

            try {
                chatUpdateService.updateLastReadChatId(chatroomId, member, lastChatId);
            } catch (Exception e) {
                log.error("마지막으로 확인한 메시지 식별자 업데이트 실패. chatroomId={}, memberId={}, {}", chatroomId, member.getMemberId(), e);
            }
        }

    }

    /**
     * 주어진 회원 ID에 해당하는 회원 정보를 반환합니다.
     * 회원 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 상대방의 현재 활성화 상태를 확인합니다.
     */
    private boolean getOpponentStatus(Long chatroomId, Member member) {
        return chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)
                .filter(status -> status == ChatroomMemberStatus.ACTIVE)
                .isPresent();
    }

    /**
     * 주어진 채팅방 ID와 회원 정보를 기반으로 상대방이 차단되었는지 여부를 확인합니다.
     */
    private boolean getOpponentBlocked(Long chatroomId, Member member) {
        Chatroom chatroom = chatroomRepository.findById(chatroomId).orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        return chatroomMemberRepository.checkBlockExists(chatroom, member);
    }

    /**
     * 주어진 회원이 특정 채팅방에서 활성 상태인지 검증합니다.
     * 활성 상태가 아닌 경우 예외를 발생시킵니다.
     */
    private ChatroomMember validateActiveChatroomMember(Member member, Long chatroomId) {
        ChatroomMember chatroomMember = chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));

        // 현재 회원의 상태가 나감이면 오류 반환
        if (chatroomMember.getChatroomMemberStatus() != ChatroomMemberStatus.ACTIVE) {
            throw new CustomException(ErrorCode.CHATROOM_MEMBER_INACTIVE);
        }

        return chatroomMember;
    }
}
