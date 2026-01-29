package com.project200.undabang.comment.service;

import com.project200.undabang.comment.dto.response.CommentResponse;

import java.util.List;

public interface CommentQueryService {

    List<CommentResponse> getComments(Long feedId);
}
