package com.project200.undabang.chat.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chatrooms")
public class Chatroom {
    @Id
    @Column(name = "chatroom_id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "chatroom_created_at", nullable = false)
    private LocalDateTime chatroomCreatedAt = LocalDateTime.now();

    @Column(name = "chatroom_deleted_at")
    private LocalDateTime chatroomDeletedAt;

}