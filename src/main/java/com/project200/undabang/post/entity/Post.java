package com.project200.undabang.post.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_type_id", nullable = false, updatable = false)
    private PostType postType;

    @NotNull
    @Lob
    @Column(name = "post_content", nullable = false, columnDefinition = "text")
    private String postContent;

    @org.hibernate.annotations.Comment("관리자 제제 시 1")
    @ColumnDefault("0")
    @Column(name = "post_is_reported")
    @Builder.Default
    private Boolean postIsReported = false;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "post_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime postCreatedAt = LocalDateTime.now();

    @Column(name = "post_deleted_at")
    private LocalDateTime postDeletedAt;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "post_likes_cnt", nullable = false)
    @Builder.Default
    private Integer postLikesCnt = 0;

}