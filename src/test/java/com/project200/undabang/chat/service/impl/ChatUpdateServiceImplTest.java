package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
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

    @Nested
    @DisplayName("updateLastReadChatId 메소드는")
    class Describe_updateLastReadChatId {

        @Test
        @DisplayName("일치하는 ChatroomMember가 존재할 경우, 마지막으로 읽은 메시지 ID를 업데이트한다")
        void it_updates_last_read_chat_id_when_member_exists() {
            // given
            Long chatroomId = 1L;
            UUID memberId = UUID.randomUUID();
            Long lastReadChatIdToUpdate = 100L;

            // 실제 ChatroomMember 객체를 생성하여 상태 변경을 확인할 수 있도록 함
            ChatroomMember chatroomMember = spy(ChatroomMember.builder()
                    .lastReadChatId(50L) // 초기 상태는 50
                    .build());

            // Repository가 이 chatroomMember를 반환하도록 설정
            given(chatroomMemberRepository.findByChatroom_IdAndMember_MemberId(chatroomId, memberId))
                    .willReturn(Optional.of(chatroomMember));

            // when
            chatUpdateService.updateLastReadChatId(chatroomId, memberId, lastReadChatIdToUpdate);

            // then
            // 1. Repository의 find 메소드가 올바른 인자와 함께 호출되었는지 검증
            then(chatroomMemberRepository).should(times(1)).findByChatroom_IdAndMember_MemberId(chatroomId, memberId);

            // 2. 찾아낸 ChatroomMember 객체의 updateLastReadChatId 메소드가 호출되었는지 검증
            then(chatroomMember).should(times(1)).updateLastReadChatId(lastReadChatIdToUpdate);

            // 3. (선택적) 실제 객체의 상태가 변경되었는지 확인하여 더 확실하게 검증
            assertThat(chatroomMember.getLastReadChatId()).isEqualTo(lastReadChatIdToUpdate);
        }

        @Test
        @DisplayName("일치하는 ChatroomMember가 존재하지 않을 경우, 아무런 동작도 하지 않는다")
        void it_does_nothing_when_member_does_not_exist() {
            // given
            Long chatroomId = 1L;
            UUID memberId = UUID.randomUUID();
            Long lastReadChatIdToUpdate = 100L;

            // Repository가 빈 Optional을 반환하도록 설정
            given(chatroomMemberRepository.findByChatroom_IdAndMember_MemberId(chatroomId, memberId))
                    .willReturn(Optional.empty());

            // when
            chatUpdateService.updateLastReadChatId(chatroomId, memberId, lastReadChatIdToUpdate);

            // then
            // find 메소드는 호출되지만, 그 이후의 어떤 상호작용도 없어야 함
            then(chatroomMemberRepository).should(times(1)).findByChatroom_IdAndMember_MemberId(chatroomId, memberId);

            // 추가적인 검증을 위해 다른 Mock 객체(만약 있다면)와의 상호작용이 없는지 확인
            // verifyNoMoreInteractions(someOtherMock);
        }
    }
}