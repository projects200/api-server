package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.event.ChatMessageCreatedEvent;
import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.dto.response.CreateMessageResponse;
import com.project200.undabang.chat.entity.*;
import com.project200.undabang.chat.repository.ChatRepository;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberBlockRepository;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
    private final MemberBlockRepository memberBlockRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager em;

    private final int DIRECT_CHAT_MAX_MEMBER_COUNT = 2;

    /**
     * 주어진 요청 정보를 기반으로 채팅방을 생성하거나 기존 채팅방을 반환합니다.
     * 동일 사용자가 자신과의 채팅을 시도하면 예외를 발생시킵니다.
     * 두 사용자가 동시에 채팅방을 생성할 가능성을 방지하기 위해 비관적 락을 적용합니다.
     */
    @Override
    @Transactional
    public CreateChatroomResponse createChatroom(CreateChatroomRequest request) {
        UUID currentMemberId = UserContextHolder.getUserId();
        UUID targetMemberId = request.getReceiverId();

        if (currentMemberId.equals(targetMemberId)) {
            throw new CustomException(ErrorCode.SELF_CHAT_NOT_ALLOWED);
        }

        Member currentMember = getMember(currentMemberId);
        Member targetMember = getMember(targetMemberId);

        // 차단 관계가 있는 경우 채팅방 생성 금지
        if (memberBlockRepository.checkMemberBlockExists(currentMember, targetMember)) {
            throw new CustomException(ErrorCode.CHATROOM_CREATE_BLOCKED);
        }

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

        // 채팅 생성되었다는 이벤트 생성. 이 시점에서는 트랜잭션이 커밋되지 않았을 수 있으므로 @TransactionalEventListener(phase = AFTER_COMMIT)을 사용해야 함
        eventPublisher.publishEvent(ChatMessageCreatedEvent.from(savedChat));

        return CreateMessageResponse.of(savedChat.getId());
    }

    /**
     * 주어진 채팅방 ID에 해당하는 채팅방에서 사용자를 탈퇴 처리하고,
     * 채팅방 상태를 업데이트하며, 시스템 메시지를 생성하여 저장합니다.
     */
    @Override
    @Transactional
    public void leaveChatroom(Long chatroomId) {
        Member member = getMember(UserContextHolder.getUserId());
        ChatroomMember chatroomMember = chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));

        // 이미 나간 회원의 경우 빠르게 반환해서 추가 연산을 줄임
        if (chatroomMember.getChatroomMemberStatus() == ChatroomMemberStatus.LEFT) {
            return;
        }

        chatroomMember.updateMemberStatus(ChatroomMemberStatus.LEFT);

        Chatroom chatroom = chatroomMember.getChatroom();

        // 활성화된 회원이 없다면 채팅방 논리적 삭제
        if (chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE) == 0) {
            chatroom.deleteChatroom();
        }

        Chat systemChat = Chat.ofRoomCreation(SystemMessage.USER_LEFT_CHAT_ROOM.format(member.getMemberNickname()), chatroom);
        chatRepository.save(systemChat);
    }

    /**
     * 주어진 현재 사용자와 대상 사용자를 기준으로 채팅방을 검색하거나,
     * 존재하지 않을 경우 새롭게 생성하여 반환합니다.
     */
    private Chatroom findOrCreateChatroom(Member currentMember, Member targetMember) {

        Optional<Chatroom> existingChatroom = chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember);

        if (existingChatroom.isPresent()) {
            return findExistingChatroom(existingChatroom.get(), currentMember, targetMember);
        } else {
            return createNewChatroomWithLock(currentMember, targetMember);
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

        // 데드락 방지를 위해 ID를 기준으로 정렬하여 업데이트 순서를 보장 (기존 변경감지 코드는 데드락 유발)
        // 그 후, flush를 사용하여 DB에 즉시 반영
        List<ChatroomMember> membersToUpdate = Stream.of(currentChatroomMember, targetChatroomMember)
                .sorted(Comparator.comparing(ChatroomMember::getChatroomMemberId))
                .toList();

        for (ChatroomMember cm : membersToUpdate) {
            cm.updateMemberStatus(ChatroomMemberStatus.ACTIVE);
            em.flush();
        }

        if (hasReactivated) {
            Chat systemChat = Chat.ofRoomCreation(SystemMessage.USER_CREATED_CHAT_ROOM.format(currentMember.getMemberNickname()), chatroom);
            chatRepository.save(systemChat);
        }

        return chatroom;
    }

    /**
     * 현재 멤버와 대상 멤버 간의 새 채팅방을 생성합니다.
     * 비관적 락(pessimistic lock)을 사용하여 멀티스레드 환경에서의 동시성을 제어하고,
     * 안전하게 새로운 채팅방을 생성하도록 구현되었습니다.
     */
    private Chatroom createNewChatroomWithLock(Member currentMember, Member targetMember) {
        // 3. 신규 생성이 필요할 때만 비관적 락을 건다.
        List<UUID> sortedMemberIdList = Stream.of(currentMember.getMemberId(), targetMember.getMemberId()).sorted().toList();
        List<Member> pessimisticLockedMemberList = memberRepository.findAllByIdWithPessimisticLock(sortedMemberIdList);

        if (pessimisticLockedMemberList.size() != DIRECT_CHAT_MAX_MEMBER_COUNT) {
            // 이 부분은 이제 거의 발생하지 않아야 하지만 안전장치로 둔다.
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 4. Double-checked locking: 락을 획득한 후, 그 사이에 다른 스레드가 채팅방을 만들었는지 다시 한번 확인
        Optional<Chatroom> recheckChatroom = chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember);
        if (recheckChatroom.isPresent()) {
            return recheckChatroom.get(); // 이미 생성되었다면 그것을 반환
        }

        // 5. 이제서야 안전하게 새로운 채팅방을 생성
        return createNewChatroom(currentMember, targetMember);
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
        validateMemberBlockExists(chatroom, member);
        validateOtherMemberStatus(chatroom, member); // 채팅방에 나간 회원이 있는지 검사하는 헬퍼 메소드
    }

    /**
     * 채팅방과 회원 정보를 기반으로 차단 여부를 확인하는 메서드.
     * 차단 관계가 존재하는 회원이 메시지를 전송하려 할 경우 예외를 발생시킵니다.
     */
    private void validateMemberBlockExists(Chatroom chatroom, Member member) {
        if (chatroomMemberRepository.checkBlockExists(chatroom, member)) {
            throw new CustomException(ErrorCode.MESSAGE_SEND_BLOCKED);
        }
    }

    /**
     * 다른 멤버의 상태를 검증하여 활성 멤버가 1명일 경우(본인만 남은 경우) 예외를 발생시킵니다.
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
