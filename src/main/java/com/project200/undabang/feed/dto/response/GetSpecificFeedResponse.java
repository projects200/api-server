package com.project200.undabang.feed.dto.response;

import com.project200.undabang.feed.dto.record.FeedDetailRecord;
import com.project200.undabang.feed.dto.record.FeedPictureRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSpecificFeedResponse {
    private long feedId;
    private String feedContent;
    private int feedLikesCount;
    private int feedCommentsCount;
    private long feedTypeId;
    private String feedTypeName;
    private String feedTypeDesc;
    private LocalDateTime feedCreatedAt;
    private boolean feedIsLiked;
    private boolean feedHasCommented;
    private UUID memberId;
    private String nickname;
    private String profileUrl;
    private String thumbnailUrl;
    private List<FeedPictureRecord> feedPictures;

    public static GetSpecificFeedResponse from(FeedDetailRecord record, List<FeedPictureRecord> feedPictures) {
        return GetSpecificFeedResponse.builder()
                .feedId(record.feedId())
                .feedContent(record.feedContent())
                .feedLikesCount(record.feedLikesCount())
                .feedCommentsCount(record.feedCommentsCount())
                .feedTypeId(record.feedTypeId())
                .feedTypeName(record.feedTypeName())
                .feedTypeDesc(record.feedTypeDesc())
                .feedCreatedAt(record.feedCreatedAt())
                .feedIsLiked(record.feedIsLiked())
                .feedHasCommented(record.feedHasCommented())
                .memberId(record.memberId())
                .nickname(record.nickname())
                .profileUrl(record.profileUrl())
                .thumbnailUrl(record.thumbnailUrl())
                .feedPictures(feedPictures)
                .build();
    }
}
