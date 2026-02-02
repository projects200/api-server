package com.project200.undabang.feed.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "feeds")
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_type_id")
    private FeedType feedType;

    @NotNull
    @Column(name = "feed_content", nullable = false, columnDefinition = "TEXT")
    private String feedContent;

    @NotNull
    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "feed_likes_cnt", nullable = false)
    private Integer likesCount = 0;

    @NotNull
    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "feed_comments_cnt", nullable = false)
    private Integer commentsCount = 0;

    @NotNull
    @Builder.Default
    @Column(name = "feed_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "feed_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "feed_deleted_at")
    private LocalDateTime deletedAt;

    public static Feed create(Member member, String feedContent, FeedType feedType) {
        return Feed.builder()
                .member(member)
                .feedContent(feedContent)
                .feedType(feedType)
                .likesCount(0)
                .commentsCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void update(String feedContent, FeedType feedType) {
        this.feedContent = feedContent;
        this.feedType = feedType;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.updatedAt = LocalDateTime.now();
        this.deletedAt = LocalDateTime.now();
    }
}