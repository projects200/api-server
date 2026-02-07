package com.project200.undabang.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.") String content,
        Long parentCommentId) {
}
