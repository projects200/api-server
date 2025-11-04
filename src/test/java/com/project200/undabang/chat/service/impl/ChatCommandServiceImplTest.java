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
import com.project200.undabang.member.repository.MemberBlockRepository;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
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

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @Mock
    private EntityManager em;

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
                given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(false); // 차단 안 함
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
                given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MESSAGE_SEND_BLOCKED.getMessage());

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
                given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(false);
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
        List<Member> sortedMembers = Stream.of(member1, member2)
                .sorted((m1, m2) -> m1.getMemberId().compareTo(m2.getMemberId()))
                .toList();
        given(memberRepository.findAllByIdWithPessimisticLock(sortedIds)).willReturn(sortedMembers);
    }

    private ChatroomMember createChatroomMemberWithId(Long id, Chatroom chatroom, Member member, ChatroomMemberStatus status) {
        return ChatroomMember.builder()
                .chatroomMemberId(id)
                .chatroom(chatroom)
                .member(member)
                .chatroomMemberStatus(status)
                .build();
    }

    @Nested
    @DisplayName("createChatroom 메소드는")
    class Describe_createChatroom {

        @Test
        @DisplayName("상호 차단된 관계일 경우 예외를 발생시킨다")
        void it_throws_exception_when_members_are_blocked() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                // [수정] findById Mocking 추가 (리팩토링으로 인해 항상 호출됨)
                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));

                given(memberBlockRepository.checkMemberBlockExists(currentMember, targetMember)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_CREATE_BLOCKED.getMessage());

                // then
                verify(chatroomRepository, never()).findChatroomBetweenMembers(any(), any());
                verify(memberRepository, never()).findAllByIdWithPessimisticLock(anyList());
            }
        }

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
        @DisplayName("기존 채팅방이 없을 경우 새로운 채팅방을 생성한다")
        void it_creates_new_chatroom_if_not_exists() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom newChatroom = createChatroom(1L);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(any(), any())).willReturn(false);

                // [수정] Double-checked locking 시나리오를 위해 순차적으로 다른 값을 반환하도록 설정
                // 1차 조회 (락 이전) -> 없음, 2차 조회 (락 이후) -> 없음
                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember))
                        .willReturn(Optional.empty())
                        .willReturn(Optional.empty());

                mockMemberLocking(currentMember, targetMember);
                given(chatroomRepository.save(any(Chatroom.class))).willReturn(newChatroom);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(newChatroom.getId());
                then(memberRepository).should(times(1)).findAllByIdWithPessimisticLock(anyList());
                then(chatroomRepository).should(times(1)).save(any(Chatroom.class));
                then(chatroomMemberRepository).should(times(1)).saveAll(any());
            }
        }

        // [추가] Double-checked locking 로직 검증을 위한 테스트 케이스
        @Test
        @DisplayName("신규 생성 시, 락을 잡은 후 다른 스레드가 생성한 채팅방을 발견하면 중복 생성하지 않는다")
        void it_returns_existing_chatroom_when_found_after_lock() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom existingChatroom = createChatroom(1L);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(any(), any())).willReturn(false);

                // [핵심] 1차 조회 시에는 채팅방이 없다가, 2차 조회(락 획득 후) 시에는 채팅방이 있도록 설정
                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember))
                        .willReturn(Optional.empty()) // 락 이전 조회
                        .willReturn(Optional.of(existingChatroom)); // 락 이후 조회

                mockMemberLocking(currentMember, targetMember);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                // 비관적 락은 실행되었어야 함
                then(memberRepository).should(times(1)).findAllByIdWithPessimisticLock(anyList());
                // 하지만 새로운 채팅방 생성(save) 로직은 실행되지 않았어야 함
                then(chatroomRepository).should(never()).save(any(Chatroom.class));
                then(chatroomMemberRepository).should(never()).saveAll(any());
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

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));

                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(2L);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                then(memberRepository).should(never()).findAllByIdWithPessimisticLock(anyList());
                verify(chatroomMemberRepository, never()).saveAll(any());
                verify(chatRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("기존 채팅방에 상대방만 나간 상태일 경우 재활성화하고 채팅방을 반환한다")
        void it_reactivates_and_returns_chatroom_when_target_member_left() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom existingChatroom = createChatroom(1L);
            ChatroomMember currentChatroomMember = spy(createChatroomMemberWithId(2L, existingChatroom, currentMember, ChatroomMemberStatus.ACTIVE));
            ChatroomMember targetChatroomMember = spy(createChatroomMemberWithId(3L, existingChatroom, targetMember, ChatroomMemberStatus.LEFT));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));

                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, currentMember)).willReturn(Optional.of(currentChatroomMember));
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, targetMember)).willReturn(Optional.of(targetChatroomMember));

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                then(currentChatroomMember).should().updateMemberStatus(ChatroomMemberStatus.ACTIVE);
                then(targetChatroomMember).should().updateMemberStatus(ChatroomMemberStatus.ACTIVE);
                then(memberRepository).should(never()).findAllByIdWithPessimisticLock(anyList());
                // hasReactivated가 false이므로 시스템 메시지는 생성되지 않아야 함
                verify(chatRepository, never()).save(any(Chat.class));
            }
        }

        @Test
        @DisplayName("신규 생성 시, 비관적 락 이후 멤버 수가 2가 아니면 예외를 발생시킨다")
        void it_throws_exception_when_pessimistic_lock_fails_on_creation() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            List<UUID> sortedIds = Stream.of(currentMember.getMemberId(), targetMember.getMemberId()).sorted().toList();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));

                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.empty());
                given(memberRepository.findAllByIdWithPessimisticLock(sortedIds)).willReturn(Collections.emptyList());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            }
        }

        @Test
        @DisplayName("재활성화 시, 채팅방 멤버 데이터를 찾지 못하면 예외를 발생시킨다")
        void it_throws_exception_when_chatroom_member_not_found_on_reactivation() {
            // given: 재활성화가 필요한 상황 설정
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId());
            Chatroom existingChatroom = createChatroom(1L);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                // 공통 Mocking 설정
                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));

                // reActiveChatroom 진입 조건 설정
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);

                // [핵심] getChatroomMember 메서드에서 예외가 발생하도록 Optional.empty() 반환 설정
                // currentMember에 대한 ChatroomMember 정보가 DB에 없는 비정상적인 상황을 시뮬레이션
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, currentMember)).willReturn(Optional.empty());

                // when & then: 예외 발생 검증
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());
            }
        }
    }
}
