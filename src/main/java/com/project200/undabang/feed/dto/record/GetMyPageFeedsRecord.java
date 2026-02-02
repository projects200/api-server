package com.project200.undabang.feed.dto.record;

import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record GetMyPageFeedsRecord(long feedId,
                                   String feedContent,
                                   int feedLikesCount,
                                   int feedCommentsCount,
                                   long feedTypeId,
                                   String feedTypeName,
                                   String feedTypeDesc,
                                   LocalDateTime feedCreatedAt,
                                   boolean feedIsLiked,
                                   boolean feedHasCommented,
                                   List<FeedPictureRecord> feedPictures) {

    public static GetMyPageFeedsRecord from(FeedDetailResponse response) {
        return GetMyPageFeedsRecord.builder()
                .feedId(response.getFeedId())
                .feedContent(response.getFeedContent())
                .feedLikesCount(response.getFeedLikesCount())
                .feedCommentsCount(response.getFeedCommentsCount())
                .feedTypeId(response.getFeedTypeId())
                .feedTypeName(response.getFeedTypeName())
                .feedTypeDesc(response.getFeedTypeDesc())
                .feedCreatedAt(response.getFeedCreatedAt())
                .feedIsLiked(response.isFeedIsLiked())
                .feedHasCommented(response.isFeedHasCommented())
                .feedPictures(response.getFeedPictures())
                .build();
    }
}
