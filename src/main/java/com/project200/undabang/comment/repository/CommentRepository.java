package com.project200.undabang.comment.repository;

import com.project200.undabang.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    List<Comment> findByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(Long feedId);
}
