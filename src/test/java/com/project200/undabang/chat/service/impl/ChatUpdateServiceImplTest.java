package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChatUpdateServiceImplTest {

    @InjectMocks
    private ChatUpdateServiceImpl chatUpdateService;

    @Mock
    private ChatroomMemberRepository chatroomMemberRepository;

    private Member createMember(UUID memberId) {
        return Member.builder().memberId(memberId).build();
    }

    @Nested
    @DisplayName("updateLastReadChatId 메소드는")
    class Describe_updateLastReadChatId {

        @Test
        @DisplayName("일치하는 ChatroomMember가 존재할 경우, 마지막으로 읽은 메시지 ID를 업데이트한다")
        void it_updates_last_read_chat_id_when_member_exists() {
            // given
            Long chatroomId = 1L;
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId); // UUID 대신 Member 객체 생성
            Long lastReadChatIdToUpdate = 100L;

            ChatroomMember chatroomMember = spy(ChatroomMember.builder()
                    .lastReadChatId(50L)
                    .build());

            // [수정] 변경된 Repository 메소드를 Mocking
            given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member))
                    .willReturn(Optional.of(chatroomMember));

            // when
            // [수정] memberId 대신 member 객체를 전달
            chatUpdateService.updateLastReadChatId(chatroomId, member, lastReadChatIdToUpdate);

            // then
            // [수정] 변경된 Repository 메소드 호출을 검증
            then(chatroomMemberRepository).should(times(1)).findByChatroom_IdAndMember(chatroomId, member);

            then(chatroomMember).should(times(1)).updateLastReadChatId(lastReadChatIdToUpdate);

            assertThat(chatroomMember.getLastReadChatId()).isEqualTo(lastReadChatIdToUpdate);
        }

        @Test
        @DisplayName("일치하는 ChatroomMember가 존재하지 않을 경우, 아무런 동작도 하지 않는다")
        void it_does_nothing_when_member_does_not_exist() {
            // given
            Long chatroomId = 1L;
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId); // UUID 대신 Member 객체 생성
            Long lastReadChatIdToUpdate = 100L;

            // [수정] 변경된 Repository 메소드를 Mocking
            given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member))
                    .willReturn(Optional.empty());

            // when
            // [수정] memberId 대신 member 객체를 전달
            chatUpdateService.updateLastReadChatId(chatroomId, member, lastReadChatIdToUpdate);

            // then
            // [수정] 변경된 Repository 메소드 호출을 검증
            then(chatroomMemberRepository).should(times(1)).findByChatroom_IdAndMember(chatroomId, member);

            // ifPresent 람다식이 실행되지 않으므로, 추가적인 상호작용이 없어야 함
            // verifyNoMoreInteractions(someOtherMock);
        }
    }
}