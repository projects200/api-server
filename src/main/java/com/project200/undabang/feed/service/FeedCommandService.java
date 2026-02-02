package com.project200.undabang.feed.service;

import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.request.UpdateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
import com.project200.undabang.feed.dto.response.UpdateFeedResponse;

public interface FeedCommandService {
    CreateFeedResponse createMemberFeed(CreateFeedRequest request);

    UpdateFeedResponse updateMemberFeed(Long feedId, UpdateFeedRequest request);
}
