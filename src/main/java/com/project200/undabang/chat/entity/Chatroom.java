package com.project200.undabang.chat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "chatrooms")
public class Chatroom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "last_chat_content", length = 255)
    private String lastChatContent;

    @Column(name = "last_chat_received_at")
    private LocalDateTime lastChatReceivedAt;

    @NotNull
    @Builder.Default
    @Column(name = "chatroom_created_at", nullable = false, updatable = false)
    private LocalDateTime chatroomCreatedAt = LocalDateTime.now();

    @Column(name = "chatroom_deleted_at")
    private LocalDateTime chatroomDeletedAt;

    @OneToMany(mappedBy = "chatroom")
    @Builder.Default
    private List<Chat> chats = new ArrayList<>();

    @OneToMany(mappedBy = "chatroom")
    @Builder.Default
    private List<ChatroomMember> chatroomMembers = new ArrayList<>();

    /**
     * 새로운 Chatroom 객체를 생성하여 반환합니다.
     */
    public static Chatroom createChatroom() {
        return new Chatroom();
    }

    /**
     * 마지막 채팅 내용을 업데이트하고 해당 업데이트 시간을 현재 시간으로 설정합니다.
     */
    public void updateLastChatContent(String lastChatContent) {
        this.lastChatContent = lastChatContent;
        this.lastChatReceivedAt = LocalDateTime.now();
    }

    /**
     * 특정 채팅방을 삭제 처리합니다.
     * 채팅방 삭제 시간(chatroomDeletedAt)을 현재 시간으로 설정하여
     * 논리적 삭제를 수행합니다.
     */
    public void deleteChatroom() {
        this.chatroomDeletedAt = LocalDateTime.now();
    }
}