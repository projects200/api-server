package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.entity.ChatType;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
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

    private GetMemberChatResponse createChatMessageResponse(Long chatId, String content, boolean isMine) {
        return new GetMemberChatResponse(
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
    @DisplayName("채팅방 메시지 조회 (getMemberChat)")
    class GetMemberChatTests {

        private Member member;
        private UUID memberId;

        @BeforeEach
        void setUp() {
            memberId = UUID.randomUUID();
            member = createMember(memberId, "test@example.com", "currentUser");
        }

        @Test
        @DisplayName("성공: 사용자가 채팅방 멤버일 경우 메시지 목록을 정상적으로 반환한다")
        void shouldReturnMessageSliceWhenUserIsMember() {
            // Given
            Long chatroomId = 1L;
            Long prevChatId = null;
            Pageable pageable = PageRequest.of(0, 30);

            List<GetMemberChatResponse> messages = List.of(createChatMessageResponse(1L, "Hello", true));
            Slice<GetMemberChatResponse> expectedSlice = new SliceImpl<>(messages, pageable, false);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // 1. 현재 사용자 조회
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

                // 2. 권한 검증 -> 성공 (true 반환)
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(true);

                // 3. 실제 메시지 조회 -> 성공
                when(chatroomRepository.getMemberChat(chatroomId, prevChatId, pageable, member)).thenReturn(expectedSlice);

                // When
                Slice<GetMemberChatResponse> result = chatQueryService.getMemberChat(chatroomId, prevChatId, pageable);

                // Then
                assertThat(result).isEqualTo(expectedSlice);
                verify(memberRepository).findById(memberId);
                verify(chatroomMemberRepository).existsByChatroom_IdAndMember(chatroomId, member);
                verify(chatroomRepository).getMemberChat(chatroomId, prevChatId, pageable, member);
            }
        }

        @Test
        @DisplayName("실패: 사용자가 채팅방 멤버가 아닐 경우 예외를 발생시킨다")
        void shouldThrowExceptionWhenUserIsNotMember() {
            // Given
            Long chatroomId = 1L;
            Long prevChatId = null;
            Pageable pageable = PageRequest.of(0, 30);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // 1. 현재 사용자 조회
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

                // 2. 권한 검증 -> 실패 (false 반환)
                when(chatroomMemberRepository.existsByChatroom_IdAndMember(chatroomId, member)).thenReturn(false);

                // When & Then
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatQueryService.getMemberChat(chatroomId, prevChatId, pageable));
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND);

                // 권한 검증에서 실패했으므로, 실제 메시지를 조회하는 chatroomRepository.getMemberChat은 절대 호출되면 안 됨
                verify(chatroomRepository, never()).getMemberChat(anyLong(), any(), any(Pageable.class), any(Member.class));
            }
        }

        @Test
        @DisplayName("실패: 사용자 ID에 해당하는 회원을 찾을 수 없을 경우 예외를 발생시킨다")
        void shouldThrowExceptionWhenMemberNotFound() {
            // Given
            Long chatroomId = 1L;
            Long prevChatId = null;
            Pageable pageable = PageRequest.of(0, 30);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // 1. 현재 사용자 조회 -> 실패
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);
                when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

                // When & Then
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatQueryService.getMemberChat(chatroomId, prevChatId, pageable));
                // getMember 메서드가 없다고 가정하고, MEMBER_NOT_FOUND를 사용
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

                // 사용자 조회에서 실패했으므로, 권한 검증 및 메시지 조회는 절대 호출되면 안 됨
                verify(chatroomMemberRepository, never()).existsByChatroom_IdAndMember(anyLong(), any(Member.class));
                verify(chatroomRepository, never()).getMemberChat(anyLong(), any(), any(Pageable.class), any(Member.class));
            }
        }
    }
}