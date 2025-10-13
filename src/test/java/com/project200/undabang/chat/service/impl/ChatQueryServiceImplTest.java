package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.response.ChatMessageDto;
import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.dto.response.GetNewChatResponse;
import com.project200.undabang.chat.entity.ChatType;
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
                .chatRoomId(1L)
                .otherMemberNickname("otherUser")
                .otherMemberProfileImageUrl("http://example.com/profile.jpg")
                .otherMemberThumbnailImageUrl("http://example.com/thumbnail.jpg")
                .lastChatContent("Hello")
                .lastChatReceivedAt(LocalDateTime.now())
                .unreadCount(1L)
                .build();
    }

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

            List<ChatMessageDto> messages = List.of(createChatMessageDto(100L, "New"));
            Slice<ChatMessageDto> messageSlice = new SliceImpl<>(messages);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(messageSlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
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

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(messageSlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));

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

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(activeMember));
                when(chatroomRepository.getMemberChat(chatroomId, null, pageable, member)).thenReturn(emptySlice);
                // [핵심] 상대방 상태가 LEFT
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.LEFT));

                // When
                GetMemberChatResponse result = chatQueryService.getMemberChat(chatroomId, null, pageable);

                // Then: .filter(...)의 false 경로 테스트
                assertThat(result.isOpponentActive()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("새로운 메시지 조회 (getNewChat)")
    class GetNewChatTests {

        private final Long chatroomId = 1L;

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
                // [핵심] 메시지가 비어있는 리스트 반환
                when(chatroomRepository.getNewMemberChat(member, chatroomId, 100L)).thenReturn(Collections.emptyList());
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));

                // When
                chatQueryService.getNewChat(chatroomId);

                // Then: if (!dtoList.isEmpty())의 false 경로 테스트
                verify(chatUpdateService, never()).updateLastReadChatId(anyLong(), any(Member.class), anyLong());
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
                // [핵심] 업데이트 시 예외 발생
                doThrow(new RuntimeException("DB Error")).when(chatUpdateService).updateLastReadChatId(anyLong(), any(Member.class), anyLong());

                // When & Then: catch (Exception e) 경로 테스트
                GetNewChatResponse result = assertDoesNotThrow(() -> chatQueryService.getNewChat(chatroomId));
                assertThat(result.getNewChats()).isEqualTo(newMessages);
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
                // [핵심] 멤버십 정보 없음
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.empty());

                // When & Then: .orElseThrow(...) 경로 테스트
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
            // [핵심] 상태가 LEFT인 ChatroomMember 객체 생성
            ChatroomMember inactiveMember = createChatroomMember(member, ChatroomMemberStatus.LEFT);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                when(memberRepository.findById(member.getMemberId())).thenReturn(Optional.of(member));

                // validateActiveChatroomMember가 inactiveMember를 찾도록 설정
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member))
                        .thenReturn(Optional.of(inactiveMember));

                // When & Then
                // CHATROOM_MEMBER_INACTIVE 예외가 발생하는지 검증
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatQueryService.getNewChat(chatroomId));
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_MEMBER_INACTIVE);

                // 권한 검증 실패 후 다른 로직은 호출되지 않아야 함
                verify(chatroomRepository, never()).getNewMemberChat(any(Member.class), anyLong(), anyLong());
            }
        }
    }
}