package com.project200.undabang.like.dto.response;

public record CreateCommentLikeResponse(
        Boolean liked,
        Integer likesCount) {
}
