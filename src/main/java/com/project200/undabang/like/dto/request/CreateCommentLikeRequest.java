package com.project200.undabang.like.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateCommentLikeRequest(
        @NotNull(message = "좋아요 여부는 필수 값입니다.")
        Boolean liked) {
}
