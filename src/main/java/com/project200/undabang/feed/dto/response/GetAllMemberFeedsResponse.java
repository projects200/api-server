package com.project200.undabang.feed.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAllMemberFeedsResponse {

    private List<FeedDetailResponse> feeds;
    private boolean hasNext;

    public static GetAllMemberFeedsResponse of(Slice<FeedDetailResponse> feeds) {
        return GetAllMemberFeedsResponse.builder()
                .feeds(feeds.getContent())
                .hasNext(feeds.hasNext())
                .build();
    }
}
