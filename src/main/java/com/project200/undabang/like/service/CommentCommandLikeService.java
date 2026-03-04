package com.project200.undabang.like.service;

import com.project200.undabang.like.dto.request.CreateCommentLikeRequest;
import com.project200.undabang.like.dto.response.CreateCommentLikeResponse;

public interface CommentCommandLikeService {

    CreateCommentLikeResponse createCommentLike(Long CommentId, CreateCommentLikeRequest request);
}
