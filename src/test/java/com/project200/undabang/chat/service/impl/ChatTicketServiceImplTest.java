package com.project200.undabang.chat.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.project200.undabang.chat.dto.response.TicketResponse;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.entity.TicketInfoRecord;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatTicketServiceImplTest {

    @InjectMocks
    private ChatTicketServiceImpl chatTicketService;

    @Mock
    private Cache<UUID, TicketInfoRecord> chatTicketCache;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatroomMemberRepository chatroomMemberRepository;

    private Member createMember() {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname("tester")
                .build();
    }

    @Nested
    @DisplayName("issueTicket 메소드는")
    class Describe_issueTicket {

        @Test
        @DisplayName("성공: 회원이 채팅방의 활성 멤버라면 티켓을 발급하고 캐시에 저장한다")
        void it_issues_ticket_successfully() {
            // given
            Long roomId = 1L;
            Member member = createMember();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.existsByChatroom_IdAndMemberAndChatroomMemberStatus(
                        roomId, member, ChatroomMemberStatus.ACTIVE)).willReturn(true);

                // when
                TicketResponse response = chatTicketService.issueTicket(roomId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getChatTicket()).isNotNull();

                // 캐시에 저장되었는지 검증
                then(chatTicketCache).should(times(1)).put(eq(response.getChatTicket()), any(TicketInfoRecord.class));
            }
        }

        @Test
        @DisplayName("실패: 회원을 찾을 수 없으면 예외를 발생시킨다")
        void it_throws_exception_when_member_not_found() {
            // given
            Long roomId = 1L;
            UUID nonExistentMemberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(nonExistentMemberId);

                given(memberRepository.findById(nonExistentMemberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatTicketService.issueTicket(roomId))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

                // 캐시 저장 로직은 호출되지 않아야 함
                then(chatTicketCache).should(never()).put(any(), any());
            }
        }

        @Test
        @DisplayName("실패: 회원이 해당 채팅방의 활성 멤버가 아니면 예외를 발생시킨다")
        void it_throws_exception_when_not_active_member() {
            // given
            Long roomId = 1L;
            Member member = createMember();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                // 채팅방 멤버가 아니거나 나간 상태(false)
                given(chatroomMemberRepository.existsByChatroom_IdAndMemberAndChatroomMemberStatus(
                        roomId, member, ChatroomMemberStatus.ACTIVE)).willReturn(false);

                // when & then
                assertThatThrownBy(() -> chatTicketService.issueTicket(roomId))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());

                then(chatTicketCache).should(never()).put(any(), any());
            }
        }
    }

    @Nested
    @DisplayName("validateTicket 메소드는")
    class Describe_validateTicket {

        @Test
        @DisplayName("성공: 유효한 티켓 ID가 주어지면 티켓 정보를 반환하고 캐시에서 제거(무효화)한다")
        void it_validates_and_invalidates_ticket() {
            // given
            UUID ticketId = UUID.randomUUID();
            TicketInfoRecord record = new TicketInfoRecord(UUID.randomUUID(), 1L);

            given(chatTicketCache.getIfPresent(ticketId)).willReturn(record);

            // when
            TicketInfoRecord result = chatTicketService.validateTicket(ticketId);

            // then
            assertThat(result).isEqualTo(record);
            // 한 번 검증된 티켓은 캐시에서 삭제되어야 함
            then(chatTicketCache).should(times(1)).invalidate(ticketId);
        }

        @Test
        @DisplayName("실패: 티켓 ID가 유효하지 않거나 만료되었으면 null을 반환하고 캐시 삭제를 시도하지 않는다")
        void it_returns_null_when_ticket_invalid() {
            // given
            UUID ticketId = UUID.randomUUID();

            // 캐시에 티켓이 없음
            given(chatTicketCache.getIfPresent(ticketId)).willReturn(null);

            // when
            TicketInfoRecord result = chatTicketService.validateTicket(ticketId);

            // then
            assertThat(result).isNull();
            // 티켓이 없으므로 invalidate도 호출되지 않아야 함
            then(chatTicketCache).should(never()).invalidate(any());
        }
    }
}