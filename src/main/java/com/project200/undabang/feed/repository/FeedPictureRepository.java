package com.project200.undabang.feed.repository;

import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedPicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedPictureRepository extends JpaRepository<FeedPicture, Long> {
    long countByFeedAndPicture_PictureDeletedAtNull(Feed feed);

    Optional<FeedPicture> findByFeedAndIdAndPicture_PictureDeletedAtNull(Feed feed, Long id);
}
