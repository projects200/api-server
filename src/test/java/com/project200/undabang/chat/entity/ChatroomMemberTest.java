package com.project200.undabang.chat.entity;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ChatroomMemberTest {

    @Nested
    @DisplayName("validateCanSendMessage 메소드는")
    class Describe_validateCanSendMessage {

        @Test
        @DisplayName("멤버의 상태가 ACTIVE이면 아무런 동작도 하지 않는다")
        void it_does_nothing_when_status_is_active() {
            // given
            ChatroomMember chatroomMember = ChatroomMember.builder()
                    .chatroomMemberStatus(ChatroomMemberStatus.ACTIVE)
                    .build();

            // when & then
            // 예외가 발생하지 않음을 확인
            assertThatCode(() -> chatroomMember.validateCanSendMessage())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("멤버의 상태가 LEFT이면 CustomException을 발생시킨다")
        void it_throws_exception_when_status_is_left() {
            // given
            ChatroomMember chatroomMember = ChatroomMember.builder()
                    .chatroomMemberStatus(ChatroomMemberStatus.LEFT) // 비활성 상태
                    .build();

            // when & then
            assertThatThrownBy(() -> chatroomMember.validateCanSendMessage())
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.CHATROOM_MEMBER_INACTIVE.getMessage());
        }

        @Test
        @DisplayName("멤버의 상태가 KICKED이면 CustomException을 발생시킨다")
        void it_throws_exception_when_status_is_kicked() {
            // given
            ChatroomMember chatroomMember = ChatroomMember.builder()
                    .chatroomMemberStatus(ChatroomMemberStatus.LEFT) // 비활성 상태
                    .build();

            // when & then
            assertThatThrownBy(() -> chatroomMember.validateCanSendMessage())
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.CHATROOM_MEMBER_INACTIVE.getMessage());
        }
    }

    @Nested
    @DisplayName("updateMemberStatus 메소드는")
    class Describe_updateMemberStatus {

        @Test
        @DisplayName("멤버의 상태를 주어진 상태로 변경한다")
        void it_updates_the_member_status() {
            // given
            ChatroomMember chatroomMember = ChatroomMember.builder()
                    .chatroomMemberStatus(ChatroomMemberStatus.ACTIVE)
                    .build();

            // when
            chatroomMember.updateMemberStatus(ChatroomMemberStatus.LEFT);

            // then
            assertThat(chatroomMember.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.LEFT);
        }
    }
}