package com.project200.undabang.feed.service;

import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;

public interface FeedCommandService {
    CreateFeedResponse createMemberFeed(CreateFeedRequest request);
}
