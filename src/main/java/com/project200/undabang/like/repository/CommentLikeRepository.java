package com.project200.undabang.like.repository;

import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.like.entity.CommentLike;
import com.project200.undabang.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentAndMember(Comment comment, Member member);
}

