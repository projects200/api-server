package com.project200.undabang.feed.repository;

import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long>, FeedRepositoryCustom {
    Optional<Feed> findByIdAndMemberAndDeletedAtNull(Long id, Member member);
}
