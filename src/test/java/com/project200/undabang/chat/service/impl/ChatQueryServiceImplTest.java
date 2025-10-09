package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
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
}