package com.project200.undabang.comment.repository;

import com.project200.undabang.comment.dto.response.CommentResponse;

import java.util.List;

public interface CommentRepositoryCustom {

    List<CommentResponse> findCommentsWithChildrenByFeedId(Long feedId);
}
