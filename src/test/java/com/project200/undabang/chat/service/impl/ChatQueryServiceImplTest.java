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

import java.time.LocalDate;
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
            Member member = createMember(memberId, "test@example.com", "testUser");
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
            Member member = createMember(memberId, "test@example.com", "testUser");

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

        @Test
        @DisplayName("성공: 사용자가 채팅방 멤버이고 상대방이 활성 상태일 경우, 올바른 응답 DTO를 반환한다")
        void shouldReturnResponseDtoWhenUserIsMemberAndOpponentIsActive() {
            // Given: 각 테스트에 필요한 모든 엔티티를 헬퍼 메서드를 통해 로컬 변수로 생성
            Long chatroomId = 1L;
            Long prevChatId = null;
            Pageable pageable = PageRequest.of(0, 30);
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");

            List<ChatMessageDto> messages = List.of(createChatMessageResponse(1L, "Hello", true));
            Slice<ChatMessageDto> messageSlice = new SliceImpl<>(messages, pageable, false);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // 1. 현재 사용자 조회
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

                // 2. 권한 검증 -> 성공
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(true);

                // 3. 메시지 목록 조회 -> 성공
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(messageSlice);

                // 4. 상대방 상태 조회 -> ACTIVE 반환
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));

                // When
                GetMemberChatResponse result = chatQueryService.getMemberChat(chatroomId, prevChatId, pageable);

                // Then
                assertThat(result.getContent()).isEqualTo(messages);
                assertThat(result.isHasNext()).isFalse();
                assertThat(result.isOpponentActive()).isTrue();

                // 모든 Mock이 올바르게 호출되었는지 검증
                verify(memberRepository).findById(memberId);
                verify(chatroomMemberRepository).existsByChatroom_IdAndMember(chatroomId, member);
                verify(chatroomRepository).getMemberChat(chatroomId, prevChatId, pageable, member);
                verify(chatroomMemberRepository).getOpponentStatusByChatroomId(chatroomId, member);
            }
        }

        @Test
        @DisplayName("성공: 상대방이 나간 상태일 경우 opponentActive가 false로 반환된다")
        void shouldReturnOpponentActiveFalseWhenOpponentHasLeft() {
            // Given
            Long chatroomId = 1L;
            Pageable pageable = PageRequest.of(0, 30);
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");
            Slice<ChatMessageDto> emptySlice = new SliceImpl<>(Collections.emptyList());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(true);
                when(chatroomRepository.getMemberChat(chatroomId, null, pageable, member)).thenReturn(emptySlice);

                // [핵심] 상대방 상태 조회 -> LEFT 반환
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.LEFT));

                // When
                GetMemberChatResponse result = chatQueryService.getMemberChat(chatroomId, null, pageable);

                // Then
                assertThat(result.isOpponentActive()).isFalse();
            }
        }

        @Test
        @DisplayName("실패: 사용자가 채팅방 멤버가 아닐 경우 예외를 발생시킨다")
        void shouldThrowExceptionWhenUserIsNotMember() {
            // Given
            Long chatroomId = 1L;
            Pageable pageable = PageRequest.of(0, 30);
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                // 권한 검증 -> 실패
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(false);

                // When & Then
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatQueryService.getMemberChat(chatroomId, null, pageable));
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND);

                verify(chatroomRepository, never()).getMemberChat(anyLong(), any(), any(Pageable.class), any(Member.class));
                verify(chatroomMemberRepository, never()).getOpponentStatusByChatroomId(anyLong(), any(Member.class));
            }
        }

        @Test
        @DisplayName("실패: 사용자 ID에 해당하는 회원을 찾을 수 없을 경우 예외를 발생시킨다")
        void shouldThrowExceptionWhenMemberNotFound() {
            // Given
            Long chatroomId = 1L;
            Pageable pageable = PageRequest.of(0, 30);
            UUID memberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                // 사용자 조회 -> 실패
                when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

                // When & Then
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatQueryService.getMemberChat(chatroomId, null, pageable));
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

                verify(chatroomMemberRepository, never()).existsByChatroom_IdAndMember(anyLong(), any(Member.class));
                verify(chatroomRepository, never()).getMemberChat(anyLong(), any(), any(Pageable.class), any(Member.class));
            }
        }

        @Test
        @DisplayName("성공: 첫 페이지 조회 시(prevChatId=null), 읽음 상태 업데이트를 호출한다")
        void shouldCallUpdateLastReadStatusOnFirstPageLoad() {
            // Given
            Long chatroomId = 1L;
            Long prevChatId = null; // 첫 페이지 조회 조건
            Pageable pageable = PageRequest.of(0, 30);
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");

            // [핵심] 메시지가 존재하는 Slice 생성
            List<ChatMessageDto> messages = List.of(
                    createChatMessageResponse(99L, "Old message", true),
                    createChatMessageResponse(100L, "New message", false) // 가장 최신 메시지
            );
            Slice<ChatMessageDto> messageSlice = new SliceImpl<>(messages, pageable, true);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(true);
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(messageSlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                // updateLastReadChatId가 void를 반환하므로 doNothing() 설정
                doNothing().when(chatUpdateService).updateLastReadChatId(anyLong(), any(Member.class), anyLong());

                // When
                chatQueryService.getMemberChat(chatroomId, prevChatId, pageable);

                // Then
                // 가장 최신 메시지 ID인 100L로 업데이트 메소드가 호출되었는지 검증
                verify(chatUpdateService, times(1)).updateLastReadChatId(chatroomId, member, 100L);
            }
        }

        @Test
        @DisplayName("성공: 이전 메시지 조회 시(prevChatId!=null), 읽음 상태 업데이트를 호출하지 않는다")
        void shouldNotCallUpdateLastReadStatusWhenScrollingUp() {
            // Given
            Long chatroomId = 1L;
            Long prevChatId = 100L; // 이전 메시지 조회 조건
            Pageable pageable = PageRequest.of(0, 30);
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");

            List<ChatMessageDto> messages = List.of(createChatMessageResponse(99L, "Old message", true));
            Slice<ChatMessageDto> messageSlice = new SliceImpl<>(messages, pageable, false);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(true);
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(messageSlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));

                // When
                chatQueryService.getMemberChat(chatroomId, prevChatId, pageable);

                // Then
                // chatUpdateService가 전혀 호출되지 않았음을 검증
                verify(chatUpdateService, never()).updateLastReadChatId(anyLong(), any(Member.class), anyLong());
            }
        }

        @Test
        @DisplayName("성공: 조회된 메시지가 없을 경우, 읽음 상태 업데이트를 호출하지 않는다")
        void shouldNotCallUpdateLastReadStatusWhenNoMessages() {
            // Given
            Long chatroomId = 1L;
            Long prevChatId = null;
            Pageable pageable = PageRequest.of(0, 30);
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");

            // [핵심] 메시지가 비어있는 Slice 생성
            Slice<ChatMessageDto> emptySlice = new SliceImpl<>(Collections.emptyList());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(true);
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(emptySlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));

                // When
                chatQueryService.getMemberChat(chatroomId, prevChatId, pageable);

                // Then
                verify(chatUpdateService, never()).updateLastReadChatId(anyLong(), any(Member.class), anyLong());
            }
        }

        @Test
        @DisplayName("성공: 읽음 상태 업데이트 중 예외가 발생해도, 메시지 조회는 정상적으로 성공한다")
        void shouldReturnMessagesEvenIfUpdateFails() {
            // Given
            Long chatroomId = 1L;
            Long prevChatId = null;
            Pageable pageable = PageRequest.of(0, 30);
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");

            List<ChatMessageDto> messages = List.of(createChatMessageResponse(100L, "New message", false));
            Slice<ChatMessageDto> messageSlice = new SliceImpl<>(messages, pageable, false);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(true);
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(messageSlice);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                // [핵심] updateLastReadChatId 호출 시 RuntimeException을 던지도록 설정
                doThrow(new RuntimeException("DB connection failed")).when(chatUpdateService).updateLastReadChatId(anyLong(), any(Member.class), anyLong());

                // When & Then
                // 예외가 발생하지 않고, 정상적으로 GetMemberChatResponse 객체를 반환하는지 확인
                GetMemberChatResponse result = assertDoesNotThrow(
                        () -> chatQueryService.getMemberChat(chatroomId, prevChatId, pageable)
                );

                // 반환된 DTO의 내용도 정상적인지 추가로 확인
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(1);
            }
        }
    }

    private Member createMember(UUID id, String email, String nickname) {
        return Member.builder()
                .memberId(id)
                .memberEmail(email)
                .memberNickname(nickname)
                .memberBday(LocalDate.of(1996, 10, 20))
                .build();
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

    private ChatMessageDto createChatMessageResponse(Long chatId, String content, boolean isMine) {
        return new ChatMessageDto(
                chatId,
                UUID.randomUUID(),
                isMine ? "currentUser" : "otherUser",
                "http://profile.url",
                "http://thumbnail.url",
                content,
                ChatType.USER,
                LocalDateTime.now(),
                isMine
        );
    }

    @Nested
    @DisplayName("새로운 메시지 조회 (getNewChat)")
    class GetNewChatTests {

        private final Long chatroomId = 1L;

        @Test
        @DisplayName("성공: 새로운 메시지가 있을 경우, 메시지 목록을 반환하고 읽음 상태를 업데이트한다")
        void shouldReturnNewMessagesAndUpdateReadStatus() {
            // Given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");

            // lastReadChatId가 50인 ChatroomMember Mock 생성
            ChatroomMember chatroomMember = mock(ChatroomMember.class);
            when(chatroomMember.getLastReadChatId()).thenReturn(50L);

            // 50 이후의 새로운 메시지 목록 Mock 생성
            List<ChatMessageDto> newMessages = List.of(
                    createChatMessageResponse(51L, "Hi", false),
                    createChatMessageResponse(52L, "Hello there", true)
            );

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                // getChatroomMember가 성공적으로 ChatroomMember를 반환하도록 설정
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(chatroomMember));
                // getNewMemberChat이 새로운 메시지 목록을 반환하도록 설정
                when(chatroomRepository.getNewMemberChat(member, chatroomId, 50L)).thenReturn(newMessages);
                // 상대방은 활성 상태로 설정
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                // updateLastReadChatId는 아무것도 하지 않도록 설정
                doNothing().when(chatUpdateService).updateLastReadChatId(anyLong(), any(Member.class), anyLong());

                // When
                GetNewChatResponse result = chatQueryService.getNewChat(chatroomId);

                // Then
                assertThat(result.getNewChats()).isEqualTo(newMessages);
                assertThat(result.isOpponentActive()).isTrue();

                // 가장 최신 메시지 ID인 52L로 업데이트 메소드가 호출되었는지 검증
                verify(chatUpdateService, times(1)).updateLastReadChatId(chatroomId, member, 52L);
            }
        }

        @Test
        @DisplayName("성공: 새로운 메시지가 없을 경우, 빈 목록을 반환하고 업데이트는 호출하지 않는다")
        void shouldReturnEmptyListWhenNoNewMessages() {
            // Given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");
            ChatroomMember chatroomMember = mock(ChatroomMember.class);
            when(chatroomMember.getLastReadChatId()).thenReturn(100L); // 100번까지 읽음

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(chatroomMember));
                // [핵심] 새로운 메시지가 없으므로 빈 리스트를 반환하도록 설정
                when(chatroomRepository.getNewMemberChat(member, chatroomId, 100L)).thenReturn(Collections.emptyList());
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));

                // When
                GetNewChatResponse result = chatQueryService.getNewChat(chatroomId);

                // Then
                assertThat(result.getNewChats()).isEmpty();
                // 메시지가 없으므로 업데이트 메소드는 호출되지 않아야 함
                verify(chatUpdateService, never()).updateLastReadChatId(anyLong(), any(Member.class), anyLong());
            }
        }

        @Test
        @DisplayName("실패: 사용자가 채팅방 멤버가 아닐 경우 예외를 발생시킨다")
        void shouldThrowExceptionWhenUserIsNotMember() {
            // Given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                // [핵심] getChatroomMember 내부의 findBy가 실패하도록 설정
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.empty());

                // When & Then
                CustomException exception = assertThrows(CustomException.class, () -> chatQueryService.getNewChat(chatroomId));
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND);

                // 멤버가 아니므로 그 이후의 어떤 repository 메소드도 호출되면 안 됨
                verify(chatroomRepository, never()).getNewMemberChat(any(Member.class), anyLong(), anyLong());
                verify(chatUpdateService, never()).updateLastReadChatId(anyLong(), any(Member.class), anyLong());
            }
        }

        @Test
        @DisplayName("성공: 읽음 상태 업데이트 중 예외가 발생해도, 새로운 메시지 목록은 정상적으로 반환된다")
        void shouldReturnNewMessagesEvenIfUpdateFails() {
            // Given
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId, "test@example.com", "currentUser");
            ChatroomMember chatroomMember = mock(ChatroomMember.class);
            when(chatroomMember.getLastReadChatId()).thenReturn(50L);
            List<ChatMessageDto> newMessages = List.of(createChatMessageResponse(51L, "Hi", false));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
                when(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).thenReturn(Optional.of(chatroomMember));
                when(chatroomRepository.getNewMemberChat(member, chatroomId, 50L)).thenReturn(newMessages);
                when(chatroomMemberRepository.getOpponentStatusByChatroomId(chatroomId, member)).thenReturn(Optional.of(ChatroomMemberStatus.ACTIVE));
                // [핵심] updateLastReadChatId 호출 시 예외를 던지도록 설정
                doThrow(new RuntimeException("DB Error")).when(chatUpdateService).updateLastReadChatId(anyLong(), any(Member.class), anyLong());

                // When & Then
                // 예외가 발생하지 않고 정상적으로 응답 객체를 반환하는지 확인
                GetNewChatResponse result = assertDoesNotThrow(() -> chatQueryService.getNewChat(chatroomId));

                // 반환된 DTO의 내용도 정상적인지 추가로 확인
                assertThat(result).isNotNull();
                assertThat(result.getNewChats()).isEqualTo(newMessages);
            }
        }
    }
}