package com.project200.undabang.feed.dto.response;

import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeedResponse {
    private long feedId;
    private String feedContent;
    private Integer feedLikesCount;
    private Integer feedCommentsCount;
    private Long feedTypeId;
    private String feedTypeName;
    private String feedTypeDesc;

    public static UpdateFeedResponse of(Feed feed) {
        Optional<FeedType> optionalFeedType = Optional.ofNullable(feed.getFeedType());

        Long feedTypeId = optionalFeedType.map(FeedType::getFeedTypeId).orElse(null);
        String feedTypeName = optionalFeedType.map(FeedType::getFeedTypeName).orElse(null);
        String feedTypeDesc = optionalFeedType.map(FeedType::getFeedTypeDesc).orElse(null);

        return UpdateFeedResponse.builder()
                .feedId(feed.getId())
                .feedContent(feed.getFeedContent())
                .feedLikesCount(feed.getLikesCount())
                .feedCommentsCount(feed.getCommentsCount())
                .feedTypeId(feedTypeId)
                .feedTypeName(feedTypeName)
                .feedTypeDesc(feedTypeDesc)
                .build();
    }
}
