package com.project200.undabang.feed.repository;

import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedPicture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedPictureRepository extends JpaRepository<FeedPicture, Long> {
    long countByFeedAndPicture_PictureDeletedAtNull(Feed feed);
}
