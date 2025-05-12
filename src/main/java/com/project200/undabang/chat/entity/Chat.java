package com.project200.undabang.chat.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private Member sender;

    @Size(max = 500)
    @NotNull
    @Column(name = "chat_content", nullable = false, length = 500)
    private String chatContent;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "chat_is_read", nullable = false)
    @Builder.Default
    private Boolean chatIsRead = false;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "chat_sended_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime chatSendedAt = LocalDateTime.now();

    @Column(name = "chat_deleted_at")
    private LocalDateTime chatDeletedAt;

}