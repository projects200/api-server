package com.project200.undabang.comment.repository;

import com.project200.undabang.comment.entity.CommentTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentTagRepository extends JpaRepository<CommentTag, Long> {
}
