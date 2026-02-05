package com.project200.undabang.like.service;

import com.project200.undabang.like.dto.CreateFeedLikeRequest;
import com.project200.undabang.like.dto.CreateFeedLikeResponse;

public interface FeedLikeCommandService {

    CreateFeedLikeResponse createFeedLike(Long FeedId, CreateFeedLikeRequest request);
}
