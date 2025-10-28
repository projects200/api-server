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
import static org.mockito.BDDMockito.then;
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

    @Nested
    @DisplayName("deleteChatroom 메소드는")
    class Describe_deleteChatroom {

        private final Long chatroomId = 1L;

        @Test
        @DisplayName("성공: 활성 멤버가 채팅방을 나가면 상태를 LEFT로 변경하고 시스템 메시지를 저장한다")
        void it_leaves_chatroom_successfully() {
            // given
            Member member = createMember();
            Chatroom chatroom = spy(createChatroom(chatroomId));
            ChatroomMember chatroomMember = spy(createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                // [핵심] 다른 활성 멤버가 1명 남아있다고 가정
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);

                // when
                chatCommandService.leaveChatroom(chatroomId);

                // then
                // 1. 멤버 상태가 LEFT로 변경되었는지 검증
                then(chatroomMember).should(times(1)).updateMemberStatus(ChatroomMemberStatus.LEFT);

                // 2. 채팅방 삭제 메소드는 호출되지 않았는지 검증
                then(chatroom).should(never()).deleteChatroom();

                // 3. 시스템 메시지가 저장되었는지 검증
                verify(chatRepository, times(1)).save(any(Chat.class));
            }
        }

        @Test
        @DisplayName("성공: 마지막 활성 멤버가 나가면 채팅방도 논리적으로 삭제한다")
        void it_deletes_chatroom_when_last_member_leaves() {
            // given
            Member member = createMember();
            Chatroom chatroom = spy(createChatroom(chatroomId));
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                // [핵심] 내가 나간 후 활성 멤버가 0명이라고 가정
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(0L);

                // when
                chatCommandService.leaveChatroom(chatroomId);

                // then
                // 1. 채팅방의 deleteChatroom 메소드가 호출되었는지 검증
                then(chatroom).should(times(1)).deleteChatroom();

                // 2. 시스템 메시지도 저장되었는지 검증
                verify(chatRepository, times(1)).save(any(Chat.class));
            }
        }

        @Test
        @DisplayName("성공: 이미 나간 멤버가 다시 나가기를 요청하면 아무 작업도 하지 않고 성공 처리한다 (멱등성)")
        void it_does_nothing_if_member_already_left() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            // [핵심] 멤버 상태가 이미 LEFT인 상황
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.LEFT);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));

                // when
                chatCommandService.leaveChatroom(chatroomId);

                // then
                // 빠른 반환이 일어났으므로, 그 이후의 어떤 DB 작업도 호출되지 않아야 함
                verify(chatroomMemberRepository, never()).countByChatroomAndChatroomMemberStatus(any(Chatroom.class), any(ChatroomMemberStatus.class));
                verify(chatRepository, never()).save(any(Chat.class));
            }
        }

        @Test
        @DisplayName("실패: 채팅방 멤버가 아닐 경우 예외를 발생시킨다")
        void it_throws_exception_if_not_a_member() {
            // given
            Member member = createMember();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                // [핵심] 멤버를 찾지 못하는 상황
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.leaveChatroom(chatroomId))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());
            }
        }
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
        @DisplayName("메시지를 성공적으로 생성하고, 채팅방의 마지막 메시지와 멤버의 마지막 읽은 ID를 업데이트한다")
        void it_creates_message_and_updates_chatroom_and_member_status() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);
            Chat savedChat = mock(Chat.class);
            given(savedChat.getId()).willReturn(100L);
            given(savedChat.getChatContent()).willReturn(messageContent);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                // [수정] 모든 검증이 통과하는 상황을 Mocking
                given(chatroomMemberRepository.checkOtherMemberBlocked(chatroom, member)).willReturn(false); // 차단 안 함
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(2L);
                given(chatRepository.save(any(Chat.class))).willReturn(savedChat);

                // when
                CreateMessageResponse response = chatCommandService.createMessage(chatroomId, request);

                // then
                assertThat(response.getChatId()).isEqualTo(100L);
                assertThat(chatroom.getLastChatContent()).isEqualTo(messageContent);
                assertThat(chatroomMember.getLastReadChatId()).isEqualTo(100L);
                verify(chatRepository).save(any(Chat.class));
            }
        }

        @Test
        @DisplayName("차단한 사용자에게 메시지를 보내려 하면 예외를 발생시킨다")
        void it_throws_exception_when_sending_message_to_blocked_user() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));

                // [핵심] 차단 검증 로직이 true를 반환하도록 설정
                given(chatroomMemberRepository.checkOtherMemberBlocked(chatroom, member)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MESSAGE_SEND_TO_BLOCKED_MEMBER.getMessage());

                // 예외 발생 시, 메시지 저장 로직은 호출되지 않아야 함
                verify(chatRepository, never()).save(any());
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
                // [수정] 차단 검사는 통과했다고 가정
                given(chatroomMemberRepository.checkOtherMemberBlocked(chatroom, member)).willReturn(false);
                // [핵심] 상대방이 나간 상황을 시뮬레이션
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
            ChatroomMember chatroomMember = spy(createChatroomMember(chatroom, member, ChatroomMemberStatus.LEFT));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));

                // when & then
                // validateCanSendMessage()가 예외를 던지는 것을 직접 검증
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBER_INACTIVE.getMessage());
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

                given(memberRepository.findById(nonExistentMemberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
            }
        }
    }
}
