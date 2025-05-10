package com.project200.undabang.post.entity;

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
@Table(name = "posts")
public class Post {
    @Id
    @Column(name = "post_id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_type_id", nullable = false)
    private PostType postType;

    @NotNull
    @Lob
    @Column(name = "post_content", nullable = false, columnDefinition = "text")
    private String postContent;

    @org.hibernate.annotations.Comment("관리자 제제 시 1")
    @ColumnDefault("0")
    @Column(name = "post_is_reported")
    private Boolean postIsReported = false;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "post_created_at", nullable = false)
    private LocalDateTime postCreatedAt = LocalDateTime.now();

    @Column(name = "post_deleted_at")
    private LocalDateTime postDeletedAt;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "post_likes_cnt", nullable = false)
    private Integer postLikesCnt = 0;

}