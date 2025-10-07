package com.project200.undabang.chat.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id", updatable = false, nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chatroom_id", nullable = false, updatable = false)
    private Chatroom chatroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", updatable = false)
    private Member sender;

    @Size(max = 500)
    @NotNull
    @Column(name = "chat_content", nullable = false, length = 500)
    private String chatContent;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "chat_type", length = 20)
    private ChatType chatType = ChatType.USER;

    @NotNull
    @Builder.Default
    @Column(name = "chat_created_at", nullable = false, updatable = false)
    private LocalDateTime chatCreatedAt = LocalDateTime.now();

}