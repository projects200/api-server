package com.project200.undabang.comment.entity;

import com.project200.undabang.comment.dto.request.CreateCommentRequest;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedType;
import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false, updatable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Comment> children = new ArrayList<>();

    @NotNull
    @Column(name = "comment_content", nullable = false)
    private String content;

    @NotNull
    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "comment_likes_cnt", nullable = false)
    private Integer likesCount = 0;

    @NotNull
    @Builder.Default
    @Column(name = "comment_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "comment_deleted_at")
    private LocalDateTime deletedAt;

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void incrementLikesCount() {
        this.likesCount++;
    }

    public void decrementLikesCount() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }

    public static Comment create(Member member, Feed feed, Comment parent, CreateCommentRequest request) {
        return Comment.builder()
                .member(member)
                .feed(feed)
                .parent(parent)
                .content(request.content())
                .build();
    }
}