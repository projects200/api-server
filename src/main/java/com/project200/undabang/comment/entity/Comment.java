package com.project200.undabang.comment.entity;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.post.entity.Post;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @Column(name = "comment_id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Size(max = 255)
    @NotNull
    @Column(name = "comment_content", nullable = false)
    private String commentContent;

    @org.hibernate.annotations.Comment("관리자 제제 시 1")
    @ColumnDefault("0")
    @Column(name = "comment_is_reported")
    private Boolean commentIsReported = false;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "comment_created_at", nullable = false)
    private LocalDateTime commentCreatedAt = LocalDateTime.now();

    @Column(name = "comment_deleted_at")
    private LocalDateTime commentDeletedAt;

}