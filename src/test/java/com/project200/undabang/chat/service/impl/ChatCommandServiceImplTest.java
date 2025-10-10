package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.dto.response.CreateMessageResponse;
import com.project200.undabang.chat.entity.Chat;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.repository.ChatRepository;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatCommandServiceImplTest {

    @InjectMocks
    private ChatCommandServiceImpl chatCommandService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ChatroomRepository chatroomRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatroomMemberRepository chatroomMemberRepository;

    @Nested
    @DisplayName("createChatroom 메소드는")
    class Describe_createChatroom {

        @Test
        @DisplayName("자기 자신과의 채팅방 생성을 시도하면 예외를 발생시킨다")
        void it_throws_exception_when_creating_chatroom_with_oneself() {
            // given
            UUID memberId = UUID.randomUUID();
            CreateChatroomRequest request = new CreateChatroomRequest(memberId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.SELF_CHAT_NOT_ALLOWED.getMessage());
            }
        }

        @Test
        @DisplayName("비관적 락으로 조회된 멤버 수가 2가 아니면 예외를 발생시킨다")
        void it_throws_exception_when_pessimistic_lock_fails() {
            // given
            UUID currentMemberId = UUID.randomUUID();
            UUID targetMemberId = UUID.randomUUID();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMemberId);
            List<UUID> sortedIds = Stream.of(currentMemberId, targetMemberId).sorted().toList();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMemberId);
                given(memberRepository.findAllByIdWithPessimisticLock(sortedIds)).willReturn(Collections.emptyList());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            }
        }

        @Test
        @DisplayName("기존 채팅방이 없을 경우 새로운 채팅방을 생성한다")
        void it_creates_new_chatroom_if_not_exists() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom newChatroom = createChatroom(1L);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());
                mockMemberLocking(currentMember, targetMember);
                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.empty());
                given(chatroomRepository.save(any(Chatroom.class))).willReturn(newChatroom);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(newChatroom.getId());
                verify(chatroomMemberRepository).saveAll(any());
            }
        }

        @Test
        @DisplayName("기존 채팅방이 있고 모든 멤버가 활성 상태이면 해당 채팅방을 반환한다")
        void it_returns_existing_chatroom_when_all_members_are_active() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom existingChatroom = createChatroom(1L);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());
                mockMemberLocking(currentMember, targetMember);
                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(2L);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                verify(chatroomMemberRepository, never()).saveAll(any());
                verify(chatRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("기존 채팅방에 나간 멤버가 있으면 멤버 상태를 활성으로 변경하고 채팅방을 반환한다")
        void it_reactivates_and_returns_chatroom_when_member_left() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom existingChatroom = createChatroom(1L);
            ChatroomMember currentChatroomMember = createChatroomMember(existingChatroom, currentMember, ChatroomMemberStatus.LEFT);
            ChatroomMember targetChatroomMember = createChatroomMember(existingChatroom, targetMember, ChatroomMemberStatus.ACTIVE);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());
                mockMemberLocking(currentMember, targetMember);
                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, currentMember)).willReturn(Optional.of(currentChatroomMember));
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, targetMember)).willReturn(Optional.of(targetChatroomMember));

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                assertThat(currentChatroomMember.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.ACTIVE);
                assertThat(targetChatroomMember.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.ACTIVE);
                verify(chatRepository).save(any(Chat.class));
            }
        }

        @Test
        @DisplayName("기존 채팅방에 상대방만 나간 상태일 경우 멤버 상태를 활성으로 변경하고 채팅방을 반환한다")
        void it_reactivates_and_returns_chatroom_when_target_member_left() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom existingChatroom = createChatroom(1L);
            ChatroomMember currentChatroomMember = createChatroomMember(existingChatroom, currentMember, ChatroomMemberStatus.ACTIVE);
            ChatroomMember targetChatroomMember = createChatroomMember(existingChatroom, targetMember, ChatroomMemberStatus.LEFT);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());
                mockMemberLocking(currentMember, targetMember);
                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, currentMember)).willReturn(Optional.of(currentChatroomMember));
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, targetMember)).willReturn(Optional.of(targetChatroomMember));

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                assertThat(currentChatroomMember.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.ACTIVE);
                assertThat(targetChatroomMember.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.ACTIVE);
                // hasReactivated가 false이므로 시스템 메시지는 생성되지 않아야 함
                verify(chatRepository, never()).save(any(Chat.class));
            }
        }

        @Test
        @DisplayName("비관적 락 이후 멤버를 찾지 못하면 예외를 발생시킨다")
        void it_throws_exception_when_member_not_found_after_lock() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            // DB 데이터와 불일치하는 상황을 가정하기 위해 다른 ID를 가진 멤버 생성
            Member anotherMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            List<UUID> sortedIds = Stream.of(currentMember.getMemberId(), targetMember.getMemberId()).sorted().toList();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());
                // 락은 2개의 멤버를 반환했지만, 그 중 하나가 예상과 다른 멤버인 경우
                given(memberRepository.findAllByIdWithPessimisticLock(sortedIds)).willReturn(List.of(currentMember, anotherMember));

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            }
        }

        @Test
        @DisplayName("비관적 락 이후 두 멤버 모두 찾지 못하면 예외를 발생시킨다")
        void it_throws_exception_when_both_members_not_found_after_lock() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            // DB 데이터와 불일치하는 상황을 가정하기 위해 다른 ID를 가진 멤버들 생성
            Member anotherMember1 = createMember();
            Member anotherMember2 = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            List<UUID> sortedIds = Stream.of(currentMember.getMemberId(), targetMember.getMemberId()).sorted().toList();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());
                // 락은 2개의 멤버를 반환했지만, 둘 다 예상과 다른 멤버인 경우
                given(memberRepository.findAllByIdWithPessimisticLock(sortedIds)).willReturn(List.of(anotherMember1, anotherMember2));

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            }
        }

        @Test
        @DisplayName("재활성화 시 채팅방 멤버를 찾지 못하면 예외를 발생시킨다")
        void it_throws_exception_when_chatroom_member_not_found_on_reactivation() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom existingChatroom = createChatroom(1L);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());
                mockMemberLocking(currentMember, targetMember);
                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);
                // getChatroomMember 메소드에서 예외가 발생하도록 Optional.empty() 반환
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, currentMember)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());
            }
        }
    }

    private Member createMember() {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname("user_" + UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    private Chatroom createChatroom(Long id) {
        return Chatroom.builder()
                .id(id)
                .build();
    }

    private ChatroomMember createChatroomMember(Chatroom chatroom, Member member, ChatroomMemberStatus status) {
        return ChatroomMember.builder()
                .chatroom(chatroom)
                .member(member)
                .chatroomMemberStatus(status)
                .build();
    }

    private void mockMemberLocking(Member member1, Member member2) {
        List<UUID> sortedIds = Stream.of(member1.getMemberId(), member2.getMemberId()).sorted().toList();
        // 정렬된 ID 순서에 맞춰 Member 객체도 정렬하여 반환하도록 설정
        List<Member> sortedMembers = Stream.of(member1, member2)
                .sorted((m1, m2) -> m1.getMemberId().compareTo(m2.getMemberId()))
                .toList();
        given(memberRepository.findAllByIdWithPessimisticLock(sortedIds)).willReturn(sortedMembers);
    }

    @Nested
    @DisplayName("createMessage 메소드는")
    class Describe_createMessage {

        private final Long chatroomId = 1L;
        private final String messageContent = "안녕하세요!";

        @Test
        @DisplayName("유효한 요청이 들어오면 메시지를 성공적으로 생성하고 저장한다")
        void it_creates_and_saves_a_message_successfully() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);

            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);
            Chat savedChat = Chat.of(messageContent, chatroom, member);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                // validateOtherMemberStatus 통과를 위해 활성 멤버 수를 2로 설정
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(2L);
                given(chatRepository.save(any(Chat.class))).willReturn(savedChat);

                // when
                CreateMessageResponse response = chatCommandService.createMessage(chatroomId, request);

                // then
                assertThat(response.getChatId()).isEqualTo(savedChat.getId());
                verify(chatRepository).save(argThat(chat ->
                        chat.getChatContent().equals(messageContent) &&
                                chat.getSender().equals(member) &&
                                chat.getChatroom().equals(chatroom)
                ));
            }
        }

        @Test
        @DisplayName("요청한 유저가 채팅방 멤버가 아니면 예외를 발생시킨다")
        void it_throws_exception_when_user_is_not_a_chatroom_member() {
            // given
            Member member = createMember();
            CreateMessageRequest request = new CreateMessageRequest(messageContent);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                // findBy 결과가 비어있도록 설정
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());
            }
        }

        @Test
        @DisplayName("채팅방에 다른 활성 멤버가 없으면 (상대방이 나갔으면) 예외를 발생시킨다")
        void it_throws_exception_when_other_member_is_inactive() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);

            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                // validateOtherMemberStatus 실패를 위해 활성 멤버 수를 1로 설정
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_OTHER_MEMBER_INACTIVE.getMessage());
            }
        }

        @Test
        @DisplayName("사용자가 메시지를 보낼 수 없는 상태(LEFT)이면 예외를 발생시킨다")
        void it_throws_exception_when_user_status_is_not_active() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);

            // 사용자의 상태가 LEFT인 ChatroomMember 생성
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.LEFT);
            // ChatroomMember의 validateCanSendMessage() 메소드가 예외를 던지도록 설정하기 위해 Mockito.spy() 사용
            ChatroomMember spiedChatroomMember = spy(chatroomMember);
            doThrow(new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND))
                    .when(spiedChatroomMember).validateCanSendMessage();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(spiedChatroomMember));

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());
            }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 요청하면 예외를 발생시킨다")
        void it_throws_exception_when_member_not_found() {
            // given
            UUID nonExistentMemberId = UUID.randomUUID();
            CreateMessageRequest request = new CreateMessageRequest("안녕하세요!");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(nonExistentMemberId);

                // [핵심] getMember()의 실패 경로를 테스트하기 위해 Optional.empty()를 반환하도록 설정
                given(memberRepository.findById(nonExistentMemberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

                // getMember() 실패 시 다른 repository는 호출되지 않아야 함
                verify(chatroomMemberRepository, never()).findByChatroom_IdAndMember(any(), any());
                verify(chatRepository, never()).save(any());
            }
        }
    }
}
