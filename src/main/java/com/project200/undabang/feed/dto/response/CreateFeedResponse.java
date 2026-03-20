package com.project200.undabang.feed.dto.response;

import com.project200.undabang.feed.entity.Feed;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedResponse {
    private Long feedId;

    public static CreateFeedResponse of(Feed feed) {
        return new CreateFeedResponse(feed.getId());
    }
}
