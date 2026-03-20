package com.project200.undabang.feed.dto.record;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedDetailRecord(
        long feedId,
        String feedContent,
        Integer feedLikesCount,
        Integer feedCommentsCount,
        Long feedTypeId,
        String feedTypeName,
        String feedTypeDesc,
        LocalDateTime feedCreatedAt,
        Boolean feedIsLiked,
        Boolean feedHasCommented,
        UUID memberId,
        String nickname,
        String thumbnailUrl,
        String profileUrl) {
}
