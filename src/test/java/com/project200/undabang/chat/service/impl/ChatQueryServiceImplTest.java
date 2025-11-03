package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.response.ChatMessageDto;
import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.dto.response.GetNewChatResponse;
import com.project200.undabang.chat.entity.ChatType;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.chat.service.ChatUpdateService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceImplTest {

    @InjectMocks
    private ChatQueryServiceImpl chatQueryService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatroomMemberRepository chatroomMemberRepository;

    @Mock
    private ChatroomRepository chatroomRepository;

    @Mock
    private ChatUpdateService chatUpdateService;

    @Nested
    @DisplayName("채팅방 목록 조회 (getMemberChatroomList)")
    class GetMemberChatroomListTests {

        @Test
        @DisplayName("성공: 사용자의 채팅방 목록을 정상적으로 반환한다")
        void shouldReturnChatroomListSuccessfully() {
            // Given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);
            List<GetMemberChatroomResponse> expectedResponse = List.of(createGetMemberChatroomResponse());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.getChatroomListByMemberId(member)).thenReturn(expectedResponse);

                // When
                List<GetMemberChatroomResponse> result = chatQueryService.getMemberChatroomList();

                // Then
                assertThat(result).isEqualTo(expectedResponse);
                verify(memberRepository).findById(memberId);
                verify(chatroomMemberRepository).getChatroomListByMemberId(member);
            }
        }

        @Test
        @DisplayName("성공: 참여중인 채팅방이 없을 경우 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenNoChatrooms() {
            // Given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.getChatroomListByMemberId(member)).thenReturn(Collections.emptyList());

                // When
                List<GetMemberChatroomResponse> result = chatQueryService.getMemberChatroomList();

                // Then
                assertThat(result).isEmpty();
                verify(memberRepository).findById(memberId);
                verify(chatroomMemberRepository).getChatroomListByMemberId(member);
            }
        }

        @Test
        @DisplayName("실패: 사용자 ID에 해당하는 회원을 찾을 수 없을 경우 예외를 발생시킨다")
        void shouldThrowExceptionWhenMemberNotFound() {
            // Given
            UUID memberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

                // When & Then
                CustomException exception = assertThrows(CustomException.class, () -> chatQueryService.getMemberChatroomList());
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

                verify(memberRepository).findById(memberId);
                verify(chatroomMemberRepository, never()).getChatroomListByMemberId(any(Member.class));
            }
        }
    }

    @Nested
    @DisplayName("새로운 메시지 조회 (getNewChat)")
    class GetNewChatTests {

        private final Long chatroomId = 1L;
        private final Chatroom chatroom = Chatroom.builder().id(chatroomId).build();

        @Test
        @DisplayName("성공: 조회된 새 메시지가 없을 경우, 읽음 상태 업데이트를 호출하지 않는다")
        void shouldNotCallUpdateWhenNewMessagesAreEmpty() {
            // Given
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE, 100L);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getNewMemberChat(member, chatroomId, 100L)).thenReturn(Collections.emptyList());
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom));
                when(chatroomMemberRepository.checkBlockExists(chatroom, member)).thenReturn(false);

                // When
                GetNewChatResponse result = chatQueryService.getNewChat(chatroomId);

                // Then
                verify(chatUpdateService, never()).updateLastReadChatId(anyLong(), any(Member.class), anyLong());
                assertThat(result.isBlockActive()).isFalse();
            }
        }

        @Test
        @DisplayName("성공: 읽음 상태 업데이트 중 예외가 발생해도, 정상적으로 응답을 반환한다")
        void shouldSucceedEvenIfUpdateFails() {
            // Given
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE, 50L);
            List<ChatMessageDto> newMessages = List.of(createChatMessageDto(51L, "Hi"));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getNewMemberChat(member, chatroomId, 50L)).thenReturn(newMessages);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom));
                when(chatroomMemberRepository.checkBlockExists(chatroom, member)).thenReturn(false);
                doThrow(new RuntimeException("DB Error")).when(chatUpdateService).updateLastReadChatId(anyLong(), any(Member.class), anyLong());

                // When & Then
                GetNewChatResponse result = assertDoesNotThrow(() -> chatQueryService.getNewChat(chatroomId));
                assertThat(result.getNewChats()).isEqualTo(newMessages);
                assertThat(result.isBlockActive()).isFalse();
            }
        }

        @Test
        @DisplayName("성공: 차단된 상태일 경우 blockActive가 true로 반환된다")
        void shouldReturnBlockActiveTrueWhenBlocked() {
            // Given
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE, 100L);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getNewMemberChat(member, chatroomId, 100L)).thenReturn(Collections.emptyList());
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom));
                when(chatroomMemberRepository.checkBlockExists(chatroom, member)).thenReturn(true); // [핵심]

                // When
                GetNewChatResponse result = chatQueryService.getNewChat(chatroomId);

                // Then
                assertThat(result.isBlockActive()).isTrue(); // [핵심]
            }
        }

        @Test
        @DisplayName("실패: 채팅방 멤버 정보를 찾을 수 없으면 예외를 발생시킨다")
        void shouldThrowExceptionWhenChatroomMemberNotFound() {
            // Given
            Member member = createMember(UUID.randomUUID());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.empty());

                // When & Then
                CustomException exception = assertThrows(CustomException.class, () -> chatQueryService.getNewChat(chatroomId));
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("실패: 사용자의 상태가 ACTIVE가 아닐 경우(나간 경우), 예외를 발생시킨다")
        void shouldThrowExceptionWhenMemberIsInactive() {
            // Given
            Long chatroomId = 1L;
            Member member = createMember(UUID.randomUUID());
            ChatroomMember inactiveMember = createChatroomMember(member, ChatroomMemberStatus.LEFT);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member))
                        .thenReturn(Optional.of(inactiveMember));

                // When & Then
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatQueryService.getNewChat(chatroomId));
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_MEMBER_INACTIVE);
                verify(chatroomRepository, never()).getNewMemberChat(any(Member.class), anyLong(), anyLong());
            }
        }

        @Test
        @DisplayName("성공: lastReadChatId가 null인 경우 -1L로 처리되어 호출된다")
        void shouldConvertNullLastReadChatIdToZero() {
            // Given
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE, null);
            List<ChatMessageDto> messages = List.of(createChatMessageDto(1L, "Message"));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getNewMemberChat(member, chatroomId, -1L)).thenReturn(messages);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom));
                when(chatroomMemberRepository.checkBlockExists(chatroom, member)).thenReturn(false);

                // When
                GetNewChatResponse result = chatQueryService.getNewChat(chatroomId);

                // Then
                verify(chatroomRepository).getNewMemberChat(member, chatroomId, -1L);
                assertThat(result.getNewChats()).isEqualTo(messages);
            }
        }

        @Test
        @DisplayName("성공: lastReadChatId가 정상값인 경우 그대로 전달된다")
        void shouldPassNormalLastReadChatId() {
            // Given
            Member member = createMember(UUID.randomUUID());
            Long lastReadId = 50L;
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE, lastReadId);
            List<ChatMessageDto> messages = List.of(createChatMessageDto(51L, "New Message"));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getNewMemberChat(member, chatroomId, lastReadId)).thenReturn(messages);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom));
                when(chatroomMemberRepository.checkBlockExists(chatroom, member)).thenReturn(false);

                // When
                GetNewChatResponse result = chatQueryService.getNewChat(chatroomId);

                // Then
                verify(chatroomRepository).getNewMemberChat(member, chatroomId, lastReadId);
                assertThat(result.getNewChats()).isEqualTo(messages);
            }
        }
    }


    @Nested
    @DisplayName("채팅방 메시지 조회 (getMemberChat)")
    class GetMemberChatTests {

        private final Long chatroomId = 1L;

        @Test
        @DisplayName("성공: 활성 멤버가 첫 페이지 조회 시, 읽음 상태를 업데이트한다")
        void shouldUpdateReadStatusOnFirstPageLoad() {
            // Given
            Long prevChatId = null; // 첫 페이지 조건
            Pageable pageable = PageRequest.of(0, 30);
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE);
            Chatroom chatroom = Chatroom.builder().id(chatroomId).build(); // [추가]

            List<ChatMessageDto> messages = List.of(createChatMessageDto(100L, "New"));
            Slice<ChatMessageDto> messageSlice = new SliceImpl<>(messages);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(messageSlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom)); // [추가]
                doNothing().when(chatUpdateService).updateLastReadChatId(anyLong(), any(Member.class), anyLong());

                // When
                chatQueryService.getMemberChat(chatroomId, prevChatId, pageable);

                // Then: if (prevChatId == null) 경로 테스트
                verify(chatUpdateService).updateLastReadChatId(chatroomId, member, 100L);
            }
        }

        @Test
        @DisplayName("성공: 이전 페이지 조회 시(prevChatId != null), 읽음 상태를 업데이트하지 않는다")
        void shouldNotUpdateReadStatusWhenPrevChatIdExists() {
            // Given
            Long prevChatId = 100L; // 이전 페이지 조건
            Pageable pageable = PageRequest.of(0, 30);
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE);
            Slice<ChatMessageDto> messageSlice = new SliceImpl<>(Collections.emptyList());
            Chatroom chatroom = Chatroom.builder().id(chatroomId).build(); // [추가]

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(messageSlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom)); // [추가]

                // When
                chatQueryService.getMemberChat(chatroomId, prevChatId, pageable);

                // Then: if (prevChatId == null)의 else 경로 테스트
                verify(chatUpdateService, never()).updateLastReadChatId(anyLong(), any(Member.class), anyLong());
            }
        }

        @Test
        @DisplayName("성공: 상대방이 나간 상태일 경우 opponentActive가 false로 반환된다")
        void shouldReturnOpponentActiveFalseWhenOpponentHasLeft() {
            // Given
            Pageable pageable = PageRequest.of(0, 30);
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE);
            Slice<ChatMessageDto> emptySlice = new SliceImpl<>(Collections.emptyList());
            Chatroom chatroom = Chatroom.builder().id(chatroomId).build(); // [추가]

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getMemberChat(chatroomId, null, pageable, member)).thenReturn(emptySlice);
                // [핵심] 상대방 상태가 LEFT
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.LEFT));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom)); // [추가]

                // When
                GetMemberChatResponse result = chatQueryService.getMemberChat(chatroomId, null, pageable);

                // Then: .filter(...)의 false 경로 테스트
                assertThat(result.isOpponentActive()).isFalse();
            }
        }

        @Test
        @DisplayName("성공: 상대방이 차단된 상태일 경우 opponentBlocked가 true로 반환된다")
        void shouldReturnOpponentBlockedTrueWhenOpponentIsBlocked() {
            // Given
            Pageable pageable = PageRequest.of(0, 30);
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE);
            Slice<ChatMessageDto> emptySlice = new SliceImpl<>(Collections.emptyList());
            Chatroom chatroom = Chatroom.builder().id(chatroomId).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getMemberChat(chatroomId, null, pageable, member)).thenReturn(emptySlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom));
                // [핵심] 상대방이 차단되었다고 가정
                when(chatroomMemberRepository.checkBlockExists(chatroom, member)).thenReturn(true);

                // When
                GetMemberChatResponse result = chatQueryService.getMemberChat(chatroomId, null, pageable);

                // Then
                assertThat(result.isBlockActive()).isTrue();
                verify(chatroomRepository).findById(chatroomId);
                verify(chatroomMemberRepository).checkBlockExists(chatroom, member);
            }
        }

        @Test
        @DisplayName("성공: 상대방이 차단되지 않은 상태일 경우 opponentBlocked가 false로 반환된다")
        void shouldReturnOpponentBlockedFalseWhenOpponentIsNotBlocked() {
            // Given
            Pageable pageable = PageRequest.of(0, 30);
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE);
            Slice<ChatMessageDto> emptySlice = new SliceImpl<>(Collections.emptyList());
            Chatroom chatroom = Chatroom.builder().id(chatroomId).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getMemberChat(chatroomId, null, pageable, member)).thenReturn(emptySlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.of(chatroom));
                // [핵심] 상대방이 차단되지 않았다고 가정
                when(chatroomMemberRepository.checkBlockExists(chatroom, member)).thenReturn(false);

                // When
                GetMemberChatResponse result = chatQueryService.getMemberChat(chatroomId, null, pageable);

                // Then
                assertThat(result.isBlockActive()).isFalse();
                verify(chatroomRepository).findById(chatroomId);
                verify(chatroomMemberRepository).checkBlockExists(chatroom, member);
            }
        }

        @Test
        @DisplayName("실패: 채팅방이 존재하지 않을 경우 CHATROOM_NOT_FOUND 예외를 발생시킨다")
        void shouldThrowExceptionWhenChatroomNotFound() {
            // Given
            Pageable pageable = PageRequest.of(0, 30);
            Member member = createMember(UUID.randomUUID());
            ChatroomMember activeMember = createChatroomMember(member, ChatroomMemberStatus.ACTIVE);
            // [추가] 비어있는 Slice 객체 생성
            Slice<ChatMessageDto> emptySlice = new SliceImpl<>(Collections.emptyList());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                // [추가] getMemberChat 호출에 대한 Mocking 추가
                when(chatroomRepository.getMemberChat(chatroomId, null, pageable, member)).thenReturn(emptySlice);
                // [핵심] chatroomRepository.findById가 Optional.empty()를 반환하도록 설정
                when(chatroomRepository.findById(chatroomId)).thenReturn(Optional.empty());

                // When & Then
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatQueryService.getMemberChat(chatroomId, null, pageable));

                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_NOT_FOUND);

                verify(chatroomRepository).findById(chatroomId);
                verify(chatroomMemberRepository, never()).checkBlockExists(any(), any());
            }
        }
    }

    private Member createMember(UUID id) {
        return Member.builder().memberId(id).memberEmail("test@test.com").memberNickname("test").build();
    }

    private ChatroomMember createChatroomMember(Member member, ChatroomMemberStatus status) {
        return createChatroomMember(member, status, null);
    }

    private ChatroomMember createChatroomMember(Member member, ChatroomMemberStatus status, Long lastReadChatId) {
        return ChatroomMember.builder().member(member).chatroomMemberStatus(status).lastReadChatId(lastReadChatId).build();
    }

    private ChatMessageDto createChatMessageDto(Long chatId, String content) {
        return new ChatMessageDto(chatId, UUID.randomUUID(), "sender", null, null, content, ChatType.USER, LocalDateTime.now(), false);
    }

    private GetMemberChatroomResponse createGetMemberChatroomResponse() {
        return GetMemberChatroomResponse.builder()
                .otherMemberId(UUID.randomUUID()) // 추가된 필드
                .chatRoomId(1L)
                .otherMemberNickname("otherUser")
                .otherMemberProfileImageUrl("http://example.com/profile.jpg")
                .otherMemberThumbnailImageUrl("http://example.com/thumbnail.jpg")
                .lastChatContent("Hello")
                .lastChatReceivedAt(LocalDateTime.now())
                .unreadCount(1L)
                .build();
    }
}