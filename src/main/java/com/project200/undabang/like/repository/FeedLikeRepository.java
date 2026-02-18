package com.project200.undabang.like.repository;

import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.like.entity.FeedLike;
import com.project200.undabang.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {
    Optional<FeedLike> findByFeedAndMember(Feed feed, Member member);
}
