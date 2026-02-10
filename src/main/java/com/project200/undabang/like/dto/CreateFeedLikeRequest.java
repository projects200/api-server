package com.project200.undabang.like.dto;

import jakarta.validation.constraints.NotNull;

public record CreateFeedLikeRequest(
        @NotNull(message = "좋아요 여부는 필수 값입니다.")
        Boolean liked) {
}
