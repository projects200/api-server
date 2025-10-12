package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.service.ChatUpdateService;
import com.project200.undabang.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatUpdateServiceImpl implements ChatUpdateService {

    private final ChatroomMemberRepository chatroomMemberRepository;

    /**
     * 제공된 채팅방 ID와 회원 ID를 기반으로 해당 회원의 마지막으로 읽은 채팅 메시지 ID를 업데이트합니다.
     * 이 메서드는 별도의 트랜잭션에서 실행되며, 필요 시 새로운 트랜잭션을 생성합니다.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLastReadChatId(Long chatId, Member member, Long lastReadChatId) {
        chatroomMemberRepository.findByChatroom_IdAndMember(chatId, member)
                .ifPresent(cm -> cm.updateLastReadChatId(lastReadChatId));
    }
}
