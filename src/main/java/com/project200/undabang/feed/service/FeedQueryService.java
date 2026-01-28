package com.project200.undabang.feed.service;

import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.dto.response.GetSpecificFeedResponse;
import org.springframework.data.domain.Pageable;

public interface FeedQueryService {
    GetAllMemberFeedsResponse getAllMemberFeeds(Long prevFeedId, Pageable pageable);

    GetSpecificFeedResponse getSpecificFeed(Long feedId);
}
