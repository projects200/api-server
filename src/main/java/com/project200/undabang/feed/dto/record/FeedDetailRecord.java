package com.project200.undabang.feed.dto.record;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedDetailRecord(
        long feedId,
        String feedContent,
        int feedLikesCount,
        int feedCommentsCount,
        long feedTypeId,
        String feedTypeName,
        String feedTypeDesc,
        LocalDateTime feedCreatedAt,
        UUID memberId,
        String nickname,
        String profileUrl,
        String thumbnailUrl) {
}
