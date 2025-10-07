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
}