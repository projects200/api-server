package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.dto.response.CreateMessageResponse;
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
     * 주어진 요청 정보를 기반으로 채팅방을 생성하거나 기존 채팅방을 반환합니다.
     * 동일 사용자가 자신과의 채팅을 시도하면 예외를 발생시킵니다.
     * 두 사용자가 동시에 채팅방을 생성할 가능성을 방지하기 위해 비관적 락을 적용합니다.
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

        // 혹시 두 회원이 동시에 채팅방을 생성할 가능성이 있기 때문에 비관적 락을 적용해서 채팅방이 동시에 생성되는 것을 방지함
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
     * 주어진 채팅방 ID와 요청 데이터를 사용하여 메시지를 생성하는 메서드입니다.
     */
    @Override
    @Transactional
    public CreateMessageResponse createMessage(Long chatroomId, CreateMessageRequest request) {
        Member member = getMember(UserContextHolder.getUserId());

        ChatroomMember chatroomMember = chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));

        validateChatroomMembersStatus(chatroomMember, member); // 채팅방에 참여한 회원들의 활성상태 체크

        Chatroom chatroom = chatroomMember.getChatroom();
        Chat savedChat = chatRepository.save(Chat.of(request.getContent(), chatroom, member)); // 채팅 엔티티 생성해서 DB에 저장

        chatroom.updateLastChatContent(savedChat.getChatContent());
        chatroomMember.updateLastReadChatId(savedChat.getId());

        return CreateMessageResponse.of(savedChat.getId());
    }

    /**
     * 주어진 현재 사용자와 대상 사용자를 기준으로 채팅방을 검색하거나,
     * 존재하지 않을 경우 새롭게 생성하여 반환합니다.
     */
    private Chatroom findOrCreateChatroom(Member currentMember, Member targetMember) {

        Optional<Chatroom> existingChatroom = chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember);

        if (existingChatroom.isPresent()) {
            Chatroom chatroom = existingChatroom.get();
            return findExistingChatroom(chatroom, currentMember, targetMember);
        } else {
            return createNewChatroom(currentMember, targetMember);
        }
    }

    /**
     * 주어진 채팅방과 현재 사용자 및 대상 사용자의 상태를 확인하여,
     * 해당 채팅방을 그대로 반환하거나 재활성화된 새로운 채팅방을 반환합니다.
     */
    private Chatroom findExistingChatroom(Chatroom chatroom, Member currentMember, Member targetMember) {
        // 이미 채팅방이 있는 경우 빠른 반환
        if (chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE) == DIRECT_CHAT_MAX_MEMBER_COUNT) {
            return chatroom;
        }

        return reActiveChatroom(chatroom, currentMember, targetMember);
    }

    /**
     * 주어진 채팅방과 현재 사용자 및 대상 사용자를 기준으로 채팅방을 재활성화합니다.
     * 채팅방 멤버의 상태를 업데이트하고, 필요 시 시스템 메시지를 생성하여 저장합니다.
     * 현재는 1:1 채팅만 있어서 문제가 없지만 추후 그룹 채팅을 개발하려면 이 부분을 수정해야 합니다.
     */
    private Chatroom reActiveChatroom(Chatroom chatroom, Member currentMember, Member targetMember) {

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
     * 주어진 현재 사용자와 대상 사용자를 기반으로 새로운 채팅방을 생성하고 반환합니다.
     * 생성된 채팅방에 현재 사용자와 대상 사용자를 멤버로 추가합니다.
     */
    private Chatroom createNewChatroom(Member currentMember, Member targetMember) {
        // 새로운 채팅방 생성
        Chatroom newChatroom = chatroomRepository.save(Chatroom.createChatroom());

        // 멤버 추가
        ChatroomMember currentChatroomMember = ChatroomMember.of(newChatroom, currentMember);
        ChatroomMember targetChatroomMember = ChatroomMember.of(newChatroom, targetMember);
        chatroomMemberRepository.saveAll(List.of(currentChatroomMember, targetChatroomMember));

        return newChatroom;
    }

    /**
     * 채팅방 멤버의 상태를 검증하는 메소드.
     * 주어진 멤버들과 채팅방의 ID 정보를 기반으로 메시지 전송 가능 여부와
     * 다른 멤버의 상태를 확인한다.
     */
    private void validateChatroomMembersStatus(ChatroomMember chatroomMember, Member member) {
        Chatroom chatroom = chatroomMember.getChatroom();

        chatroomMember.validateCanSendMessage();
        validateOtherMemberStatus(chatroom, member); // 채팅방에 나간 회원이 있는지 검사하는 헬퍼 메소드
    }

    /**
     * 다른 멤버의 상태를 검증하여 활성 멤버가 1명일 경우(본인만 남은 경우) 예외를 발생시킵니다.
     *
     * @param chatroom 검증할 채팅방 객체
     * @param member   검증 대상 멤버 객체
     */
    private void validateOtherMemberStatus(Chatroom chatroom, Member member) {
        if (chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE) == 1L) {
            throw new CustomException(ErrorCode.CHATROOM_OTHER_MEMBER_INACTIVE);
        }
    }

    /**
     * 특정 채팅방과 회원에 해당하는 ChatroomMember 엔티티를 검색합니다.
     */
    private ChatroomMember getChatroomMember(Chatroom chatroom, Member member) {
        return chatroomMemberRepository.findByChatroomAndMember(chatroom, member).orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));
    }

    /**
     * 주어진 ID를 사용하여 회원 정보를 조회합니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
