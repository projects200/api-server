package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.entity.*;
import com.project200.undabang.chat.repository.ChatRepository;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.aop.LogExecutionTime;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCommandServiceImpl implements ChatCommandService {

    private final MemberRepository memberRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatRepository chatRepository;
    private final ChatroomMemberRepository chatroomMemberRepository;
    private final int DIRECT_CHAT_MAX_MEMBER_COUNT = 2;

    /**
     * 주어진 요청 데이터를 기반으로 채팅방을 생성하거나, 이미 존재하는 채팅방을 반환합니다.
     * 현재 사용자와 대상 사용자가 동일한 경우 예외를 발생시킵니다.
     */
    @Override
    @Transactional
    @LogExecutionTime
    public CreateChatroomResponse createChatroom(CreateChatroomRequest request) {
        UUID currentMemberId = UserContextHolder.getUserId();
        UUID targetMemberId = request.getReceiverId();

        if (currentMemberId.equals(targetMemberId)) {
            throw new CustomException(ErrorCode.SELF_CHAT_NOT_ALLOWED);
        }

        // 혹시 두 회원이 동시에 채팅방을 생성할 가능성이 있기 때문에 비관적 락을 적용한다.
        List<UUID> sortedMemberIdList = Stream.of(currentMemberId, targetMemberId).sorted().toList();
        List<Member> pessimisticLockedMemberList = memberRepository.findAllByIdWithPessimisticLock(sortedMemberIdList);

        // 혹시 DB에서 락을 잘못 적용했을 경우 에러 반환
        if (pessimisticLockedMemberList.size() != DIRECT_CHAT_MAX_MEMBER_COUNT) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        Member currentMember = pessimisticLockedMemberList.stream()
                .filter(m -> m.getMemberId().equals(currentMemberId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        Member targetMember = pessimisticLockedMemberList.stream()
                .filter(m -> m.getMemberId().equals(targetMemberId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        // Lock이 설정된 상태에서 채팅방을 찾고, 생성함
        Chatroom chatroom = findOrCreateChatroom(currentMember, targetMember);

        return CreateChatroomResponse.of(chatroom.getId());
    }

    /**
     * 주어진 현재 사용자와 대상 사용자를 기준으로 채팅방을 검색하거나,
     * 존재하지 않을 경우 새롭게 생성하여 반환합니다.
     */
    private Chatroom findOrCreateChatroom(Member currentMember, Member targetMember) {

        Optional<Chatroom> existingChatroom = chatroomRepository.findChatroomInfoBetweenMembers(currentMember, targetMember);

        if (existingChatroom.isPresent()) {
            Chatroom chatroom = existingChatroom.get();
            return findExistingChatroom(chatroom, currentMember, targetMember);
        } else {
            return createNewChatroom(currentMember, targetMember);
        }
    }

    /**
     * 주어진 채팅방과 현재 사용자 및 대상 사용자를 기반으로 기존 채팅방을 검색하여 반환합니다.
     * 채팅방 멤버들이 모두 활성 상태인지 확인하고 조건에 따라 재활성화 처리를 수행합니다.
     */
    private Chatroom findExistingChatroom(Chatroom chatroom, Member currentMember, Member targetMember) {
        if (chatroomMemberRepository.checkAllMembersActive(chatroom.getId())) {
            return chatroom;
        }

        return reActiveChatroom(chatroom, currentMember, targetMember);
    }

    /**
     * 주어진 채팅방과 현재 사용자 및 대상 사용자를 기준으로 채팅방을 재활성화합니다.
     * 채팅방 멤버의 상태를 업데이트하고, 필요 시 시스템 메시지를 생성하여 저장합니다.
     */
    private Chatroom reActiveChatroom(Chatroom chatroom, Member currentMember, Member targetMember) {
        // Todo : 현재는 1:1 채팅만 있어서 문제가 없지만 추후 그룹 채팅을 개발하려면 이 부분을 리팩토링 해야 함
        ChatroomMember currentChatroomMember = getChatroomMember(chatroom, currentMember);
        ChatroomMember targetChatroomMember = getChatroomMember(chatroom, targetMember);

        boolean hasReactivated = (currentChatroomMember.getChatroomMemberStatus() == ChatroomMemberStatus.LEFT);

        currentChatroomMember.updateMemberStatus(ChatroomMemberStatus.ACTIVE);
        targetChatroomMember.updateMemberStatus(ChatroomMemberStatus.ACTIVE);

        if (hasReactivated) {
            Chat systemChat = Chat.ofRoomCreation(SystemMessage.USER_CREATED_CHAT_ROOM.format(currentMember.getMemberNickname()), chatroom);
            chatRepository.save(systemChat);
        }

        return chatroom;
    }

    /**
     * 주어진 두 멤버를 기반으로 새로운 채팅방을 생성합니다.
     * 생성된 채팅방에는 두 멤버가 자동으로 추가되며, 관련 시스템 메시지가 생성됩니다.
     */
    private Chatroom createNewChatroom(Member currentMember, Member targetMember) {
        // 채팅방 기능 개발
        Chatroom newChatroom = chatroomRepository.save(Chatroom.createChatroom());

        // 멤버 추가
        ChatroomMember currentChatroomMember = ChatroomMember.of(newChatroom, currentMember);
        ChatroomMember targetChatroomMember = ChatroomMember.of(newChatroom, targetMember);
        chatroomMemberRepository.saveAll(List.of(currentChatroomMember, targetChatroomMember));

        return newChatroom;
    }

    /**
     * 특정 채팅방과 회원에 해당하는 ChatroomMember 엔티티를 검색합니다.
     */
    private ChatroomMember getChatroomMember(Chatroom chatroom, Member member) {
        return chatroomMemberRepository.findByChatroomAndMember(chatroom, member).orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));
    }
}
