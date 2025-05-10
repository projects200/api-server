package com.project200.undabang.interaction.entity;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.post.entity.Post;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "likes")
public class Like {
    @Id
    @Column(name = "like_id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "like_created_at", nullable = false)
    private LocalDateTime likeCreatedAt = LocalDateTime.now();

    @Column(name = "like_canceled_at")
    private LocalDateTime likeCanceledAt;

}