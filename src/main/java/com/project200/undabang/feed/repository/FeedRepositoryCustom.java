package com.project200.undabang.feed.repository;

import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.dto.response.GetSpecificFeedResponse;
import com.project200.undabang.member.entity.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface FeedRepositoryCustom {
    Slice<FeedDetailResponse> getAllFeedList(Member currentMember, Long prevFeedId, Pageable pageable);
    Optional<GetSpecificFeedResponse> getSpecificFeed(Member currentMember, Long feedId);

    Slice<FeedDetailResponse> getMyPageFeedList(Member currentMember, Long prevFeedId, Pageable pageable);
}
