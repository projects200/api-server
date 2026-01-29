package com.project200.undabang.comment.service;

import com.project200.undabang.comment.dto.request.CreateCommentRequest;
import com.project200.undabang.comment.dto.response.CreateCommentResponse;

public interface CommentCommandService {

    CreateCommentResponse createComment(Long feedId, CreateCommentRequest request);

    void deleteComment(Long commentId);
}
