package com.project200.undabang.like.entity;

import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "feed_likes")
public class FeedLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_like_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false, updatable = false)
    private Feed feed;

    @NotNull
    @Builder.Default
    @Column(name = "feed_like_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}