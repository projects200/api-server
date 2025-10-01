package com.project200.undabang.openchat.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.match.service.MatchService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.openchat.dto.response.GetOpenChatUrlResponse;
import com.project200.undabang.openchat.dto.response.GetOtherMemberOpenChatUrlResponse;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import com.project200.undabang.openchat.repository.OpenChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatRoomQueryServiceImplTest {

    @Mock
    private OpenChatRoomRepository openChatRoomRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private OpenChatRoomRoomQueryServiceImpl openChatQueryService;

    private OpenChatRoom createOpenChatRoom(String url) {
        OpenChatRoom openChatRoom = mock(OpenChatRoom.class);
        // lenient stubbing: 일부 테스트에서 mock.getUrl()이 사용되지 않더라도 UnnecessaryStubbingException을 피함
        lenient().when(openChatRoom.getUrl()).thenReturn(url);
        return openChatRoom;
    }

    private Member createMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberEmail(memberId + "@test.com")
                .memberNickname("test-user-" + memberId)
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.now().minusYears(20))
                .build();
    }

    @Nested
    @DisplayName("getOpenChatroomUrl() 메소드는")
    class Describe_getOpenChatroomUrl {

        @Test
        @DisplayName("현재 사용자의 오픈 채팅 URL을 정상적으로 반환한다")
        void returns_url_for_current_user() {
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            OpenChatRoom openChatRoom = createOpenChatRoom("https://open.chat/test");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                // 불필요한 stubbing 제거: memberRepository.existsById(...) 삭제
                given(openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(member.getMemberId())).willReturn(Optional.of(openChatRoom));

                GetOpenChatUrlResponse response = openChatQueryService.getOpenChatroomUrl();

                assertThat(response).isNotNull();
                assertThat(response.getOpenChatroomUrl()).isEqualTo("https://open.chat/test");
            }
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외를 던진다")
        void throws_when_member_not_found() {
            UUID userId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(userId)).willReturn(Optional.empty());
                given(memberRepository.existsById(userId)).willReturn(false);

                assertThatThrownBy(() -> openChatQueryService.getOpenChatroomUrl())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("회원은 존재하지만 오픈 채팅방이 없으면 OPEN_CHAT_ROOM_NOT_FOUND 예외를 던진다")
        void throws_when_open_chat_not_found() {
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(member.getMemberId())).willReturn(Optional.empty());
                given(memberRepository.existsById(userId)).willReturn(true);

                assertThatThrownBy(() -> openChatQueryService.getOpenChatroomUrl())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getOtherMemberOpenChatroomUrl() 메소드는")
    class Describe_getOtherMemberOpenChatroomUrl {

        @Test
        @DisplayName("다른 회원의 오픈 채팅 URL을 반환하고 매칭 기록을 생성한다")
        void returns_other_member_url_and_creates_match() {
            UUID requesterId = UUID.randomUUID();
            UUID targetMemberId = UUID.randomUUID();
            Member targetMember = createMember(targetMemberId);
            OpenChatRoom openChatRoom = createOpenChatRoom("https://open.chat/other");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(requesterId);
                // unnecessary stubbing 제거: memberRepository.existsById(...) 삭제
                given(openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(targetMember.getMemberId())).willReturn(Optional.of(openChatRoom));

                GetOtherMemberOpenChatUrlResponse response = openChatQueryService.getOtherMemberOpenChatroomUrl(targetMemberId);

                assertThat(response).isNotNull();
                assertThat(response.getOpenChatroomUrl()).isEqualTo("https://open.chat/other");
                verify(matchService).createMatchRecordBetweenMembers(requesterId, targetMemberId);
            }
        }

        @Test
        @DisplayName("본인 요청이면 MEMBER_SELF_REQUEST_NOT_ALLOWED 예외를 던진다")
        void throws_when_requesting_self() {
            UUID userId = UUID.randomUUID();
            // 본인 요청 시 getMemberOpenChatRoom에서 MEMBER_NOT_FOUND가 발생하지 않도록 오픈채팅 존재를 목킹
            OpenChatRoom openChatRoom = createOpenChatRoom("https://open.chat/self");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(userId)).willReturn(Optional.of(openChatRoom));

                assertThatThrownBy(() -> openChatQueryService.getOtherMemberOpenChatroomUrl(userId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
            }
        }

        @Test
        @DisplayName("다른 회원 조회 시 회원이 없으면 MEMBER_NOT_FOUND 예외를 던진다")
        void throws_when_other_member_not_found() {
            UUID requesterId = UUID.randomUUID();
            UUID targetMemberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(requesterId);
                given(openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(targetMemberId)).willReturn(Optional.empty());
                given(memberRepository.existsById(targetMemberId)).willReturn(false);

                assertThatThrownBy(() -> openChatQueryService.getOtherMemberOpenChatroomUrl(targetMemberId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("다른 회원 조회 시 오픈 채팅이 없으면 OPEN_CHAT_ROOM_NOT_FOUND 예외를 던진다")
        void throws_when_other_member_open_chat_not_found() {
            UUID requesterId = UUID.randomUUID();
            UUID targetMemberId = UUID.randomUUID();
            Member targetMember = createMember(targetMemberId);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(requesterId);
                given(openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(targetMember.getMemberId())).willReturn(Optional.empty());
                given(memberRepository.existsById(targetMemberId)).willReturn(true);

                assertThatThrownBy(() -> openChatQueryService.getOtherMemberOpenChatroomUrl(targetMemberId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
            }
        }
    }
}