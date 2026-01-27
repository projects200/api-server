package com.project200.undabang.feed.repository;

import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface FeedRepositoryCustom {
    Slice<FeedDetailResponse> getAllFeedList(Long prevFeedId, Pageable pageable);
}
