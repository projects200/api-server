package com.project200.undabang.chat.entity;


import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "chatroom_members", uniqueConstraints = {
        @UniqueConstraint(
                name = "UQ_chatroom_members_chatroom_member",
                columnNames = {"chatroom_id", "member_id"}
        )
}
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatroomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_member_id")
    private Long chatroomMemberId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id")
    private Chatroom chatroom;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "chatroom_member_status", length = 10)
    private ChatroomMemberStatus chatroomMemberStatus = ChatroomMemberStatus.ACTIVE;

    @Column(name = "last_read_chat_id")
    @Builder.Default
    private Long lastReadChatId = 0L;

    public static ChatroomMember of(Chatroom chatroom, Member member) {
        return ChatroomMember.builder()
                .chatroom(chatroom)
                .member(member)
                .lastReadChatId(0L)
                .build();
    }

    public void updateMemberStatus(ChatroomMemberStatus chatroomMemberStatus) {
        this.chatroomMemberStatus = chatroomMemberStatus;
    }

    public void updateLastReadChatId(Long lastReadChatId) {
        this.lastReadChatId = lastReadChatId;
    }

    public void validateCanSendMessage() {
        if (this.chatroomMemberStatus != ChatroomMemberStatus.ACTIVE) {
            throw new CustomException(ErrorCode.CHATROOM_MEMBER_INACTIVE);
        }
    }
}